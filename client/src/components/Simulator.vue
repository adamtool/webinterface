<!--This component should 'extend' the graph editor component and modify its template a little
 bit.  There is a lot of debate within the community as to what the best way is to do this in Vue,
 because Vue 2 at the moment does not have any built in form of 'template inheritance'.
 I have settled for now on the solution suggested by "ThaDaVos" in this github issue:
 https://github.com/vuejs/vue/issues/6811#issuecomment-401049094

 To sum up what they suggesting doing, we inherit all of the properties of GraphEditor in our own
 'props', and then we pass them all to a child instance of GraphEditor using 'v-bind="$props"'.
 When we need to, we call methods on that child instance using a ref, and we add the 'simulation
 history' sidebar element using a 'slot' defined in GraphEditor.

 Here are a couple of articles detailing other possible approaches to extending/inheriting
 components.
 A broad overview:
 https://vuejsdevelopers.com/2017/06/11/vue-js-extending-components/
 A how-to using the 'pug' HTML preprocessor:
 https://vuejsdevelopers.com/2020/02/24/extending-vuejs-components-templates/
 -->
<template>
  <GraphEditor
    editorMode="Simulator"
    ref="graphEditor"
    v-bind="$props"
    @fireTransition="fireTransition"
  >
    <!--This slot here can be used to add additional UI elements, e.g. a 'load net from
    editor' button.-->
    <slot></slot>

    <!-- Simulation history sidebar -->
    <v-card
      :class="sidebarCardClass"
      :tabIndex="-1"
      @mouseover="hover = true"
      @mouseleave="hover = false"
    >
      <v-card-title
        class="flex-grow-0 flex-shrink-0"
        v-if="!isSidebarCollapsed"
      >
        Simulation History
      </v-card-title>
      <v-btn
        style=""
        :class="hideShowButtonClass"
        rounded
        @click="onClickHideShow"
      >
        <template v-if="isSidebarCollapsed">
          <v-icon :class="hover ? 'mr-1' : ''">visibility</v-icon>
          {{ hover ? 'Show history' : '' }}
        </template>
        <template v-else>
          <v-icon>visibility_off</v-icon>
          Hide
        </template>
      </v-btn>
      <v-list dense
              class="overflow-y-auto flex-grow-1"
              style="padding-top: 0;"
              ref="simulationHistoryListEl"
              v-if="!isSidebarCollapsed"
      >
        <v-list-item-group
          v-model="gameSimulationHistory.currentIndex"
          mandatory
        >
          <v-list-item
            v-for="(historyState, i) in visibleSimulationHistory.stack"
            :key="i"
            @click="onClickHistoryState(historyState, index)"
          >
            <v-list-item-content
            >
              <v-list-item-title v-text="i === 0 ? '<start>' : historyState.transitionFired">
              </v-list-item-title>
            </v-list-item-content>
          </v-list-item>
        </v-list-item-group>
      </v-list>
      <v-tooltip
        bottom
        v-if="!isSidebarCollapsed && gameSimulationHistory.stack.length > 1"
      >
        <template v-slot:activator="{ on }">
          <v-btn
            color="blue"
            style="margin-top: 10px; margin-bottom: 5px; margin-left: 5px; margin-right: 5px;"
            rounded
            @click="resetSimulation"
            v-on="on">
            Reset
          </v-btn>
        </template>
        Reset the simulation
      </v-tooltip>
      <v-tooltip
        bottom
        v-if="!isSidebarCollapsed && useModelChecking && gameSimulationHistory.stack.length > 1 && netType !== 'PETRI_NET' && cxType !== 'MODEL_CHECKING_NET'"
      >
        <template v-slot:activator="{ on }">
          <v-btn
            color="green"
            style="margin-top: 10px; margin-bottom: 5px; margin-left: 5px; margin-right: 5px;"
            rounded
            @click="saveDataFlowPdf"
            v-on="on">
            Show data flow
          </v-btn>
        </template>
        Download a PDF which shows the data flow for this firing sequence
      </v-tooltip>
    </v-card>
  </GraphEditor>
</template>

<script>
  import GraphEditor from './GraphEditor'
  import logging from '../logging'
  import Vue from 'vue'
  import {deepCopy} from '../util'

  export default {
    name: 'Simulator',
    components: {GraphEditor},
    props: {
      ...GraphEditor.props,
      // This prop is only present for model checking nets, winning strategies, etc. which are
      // created via the 'Job' system
      jobKey: {
        type: Object,
        required: false
      },
      // The type of the petri net displayed in this GraphEditor instance.
      // This corresponds to the 'NetType' enum on the server
      netType: {
        type: String,
        required: false,
        validator: function (type) {
          return ['PETRI_NET', 'PETRI_NET_WITH_TRANSITS', 'PETRI_GAME'].includes(type)
        }
      },
      cxType: {
        type: String,
        required: false,
        validator: function (cxType) {
          return ['INPUT_NET', 'MODEL_CHECKING_NET'].includes(cxType)
        }
      },
      cxData: {
        type: Object,
        required: false,
        validator: function (cxData) {
          return cxData.hasOwnProperty('loopPoint') && // Int
            cxData.hasOwnProperty('historyStack') // List<SimulationHistoryState> (See Java class)
        }
      }
    },
    data() {
      return {
        // The history of markings and transitions fired if a net is being simulated
        gameSimulationHistory: this.gameSimulationHistoryDefault(),
        isSidebarCollapsed: false,
        hover: false
      }
    },
    computed: {
      hideShowButtonClass: function () {
        if (this.isSidebarCollapsed) {
          return "hide-show-button-collapsed"
        } else {
          return "hide-show-button-expanded"
        }
      },
      sidebarCardClass: function () {
        if (this.isSidebarCollapsed) {
          return "d-flex flex-column sidebar-card-collapsed"
        } else {
          return "d-flex flex-column sidebar-card-expanded"
        }
      },
      // This is used in order to show a fake <start> state in the simulation history if no
      // transitions have yet been fired.
      visibleSimulationHistory: function () {
        if (this.gameSimulationHistory.stack.length > 0) {
          return this.gameSimulationHistory
        } else {
          return {
            currentIndex: 0,
            stack: [
              {
                // An empty state which will be shown as <start> in the list
              }
            ]
          }
        }
      }
    },
    mounted: function () {
      this.resetSimulation()
    },
    watch: {
      'gameSimulationHistory.stack': function () {
        // We must wait until Vue updates the DOM in order to scroll to the true bottom of the log.
        Vue.nextTick(() => {
          const component = this.$refs.simulationHistoryListEl
          // if (component) { // The component/element may not exist if editorMode !== 'Simulator'
          component.$el.scrollTop = component.$el.scrollHeight
          // }
        })
      },
      'gameSimulationHistory.currentIndex': function (newIndex, oldIndex) {
        const currentState = this.gameSimulationHistory.stack[newIndex]
        if (currentState) { // Maybe the history has been reset and there is no 'current state'
          this.$refs.graphEditor.applyMarking(
            currentState.marking,
            currentState.fireableTransitions)
          this.$refs.graphEditor.updateD3()
        }
      }
    },
    methods: {
      onClickHideShow: function () {
        this.isSidebarCollapsed = !this.isSidebarCollapsed;
        if (this.isSidebarCollapsed) {
          // The mouseout event does not fire if the mouse leaves the element via the
          // element shrinking in size, so we must handle that case ourselves.
          this.hover = false
        }
      },
      onClickHistoryState: function (historyState, index) {
        if (index !== this.gameSimulationHistory.currentIndex) {
          this.$refs.graphEditor.showTransitionFired({
            transitionId: historyState.transitionFired,
            wasSuccessful: true
          })
        }
      },
      saveDataFlowPdf: function () {
        // discard the first state with no transitionFired
        const firingSequence = this.gameSimulationHistory.stack.slice(1)
          .map(stackState => stackState.transitionFired)
        this.$emit('saveDataFlowPdf', {
          editorNetId: this.editorNetId,
          jobKey: this.jobKey,
          firingSequence
        })
      },
      // TODO #295 allow moving back and forth in history with arrow keys
      simulationHistoryBack: function () {
        const {currentIndex} = this.gameSimulationHistory
        this.gameSimulationHistory.currentIndex = Math.max(0, currentIndex - 1)
      },
      simulationHistoryForward: function () {
        const {stack, currentIndex} = this.gameSimulationHistory
        const newIndex = Math.min(stack.length - 1, currentIndex + 1)
        const newIndexBounded = Math.max(0, newIndex)
        this.gameSimulationHistory.currentIndex = newIndexBounded
      },
      resetSimulation: function () {
        this.$refs.graphEditor.applyMarking(
          this.graph.initialMarking,
          this.graph.fireableTransitions)
        this.$refs.graphEditor.updateD3()
        if (this.cxData) {
          this.gameSimulationHistory = {
            currentIndex: 0,
            stack: this.cxData.historyStack
          }
        } else {
          this.gameSimulationHistory = this.gameSimulationHistoryDefault()
        }
        logging.sendSuccessNotification('Reset the simulation.')
      },
      gameSimulationHistoryDefault: function () {
        return {
          currentIndex: 0, // The index of the currently selected state in the history
          /* Each game simulation state in this stack consists of an object:
          {
            marking: null, // Map[String, Number]; i.e. Map[PlaceId, TokenCount]
            transitionFired: null // The transition fired from the previous state to reach this state
          } */
          stack: []
        }
      },
      fireTransition: function (d) {
        if (this.gameSimulationHistory.stack.length === 0) {
          this.gameSimulationHistory = {
            currentIndex: 0,
            stack: [
              {
                marking: this.graph.initialMarking,
                fireableTransitions: this.graph.fireableTransitions,
                transitionFired: null
              }
            ]
          }
        }
        const {stack, currentIndex} = this.gameSimulationHistory
        const currentState = stack[currentIndex]

        const transitionId = d.id
        let requestPromise
        if (this.jobKey && this.cxType) { // The net belongs to a counter example
          requestPromise = this.restEndpoints.fireTransitionJob({
            preMarking: currentState.marking,
            jobKey: this.jobKey,
            cxType: this.cxType,
            transitionId,
            netType: this.cxType === 'MODEL_CHECKING_NET' ? 'PETRI_NET' : 'PETRI_NET_WITH_TRANSITS'
          })
        } else if (this.editorNetId) {
          requestPromise = this.restEndpoints.fireTransitionEditor({
            preMarking: currentState.marking,
            editorNetId: this.editorNetId,
            transitionId,
            netType: this.netType
          })
        } else if (this.jobKey) {
          requestPromise = this.restEndpoints.fireTransitionJob({
            preMarking: currentState.marking,
            jobKey: this.jobKey,
            transitionId,
            netType: this.netType
          })
        } else {
          throw new Error('No editorNetId or jobKey present.  The simulation can\'t be run.')
        }
        requestPromise.then(response => {
          if (response.data.status === 'success') {
            const newState = {
              marking: response.data.result.postMarking,
              fireableTransitions: response.data.result.fireableTransitions,
              transitionFired: transitionId
            }
            this.gameSimulationHistory.stack = stack.slice(0, currentIndex + 1).concat([newState])
            this.gameSimulationHistory.currentIndex = currentIndex + 1
            this.$refs.graphEditor.applyMarking(newState.marking, newState.fireableTransitions)
            this.$refs.graphEditor.updateD3()
            this.$refs.graphEditor.showTransitionFired({
              transitionId: d.id,
              wasSuccessful: true
            })
            logging.sendSuccessNotification('Fired transition ' + d.id)
          } else if (response.data.status === 'error') {
            if (response.data.errorType === 'TRANSITION_NOT_FIREABLE') {
              this.$refs.graphEditor.showTransitionFired({
                transitionId: d.id,
                wasSuccessful: false
              })
            }
            logging.sendErrorNotification(response.data.message)
          } else {
            logging.sendErrorNotification('Invalid response from server')
          }
        })
      },
      /**
       * Perform a deep copy of an arbitrary object.
       * This has some caveats.
       * See https://stackoverflow.com/questions/20662319/javascript-deep-copy-using-json
       * @param object
       * @returns A deep copy/clone of object
       */
      deepCopy: function (object) {
        return JSON.parse(JSON.stringify(object))
      }
    }
  }
</script>

<style scoped>
  .hide-show-button-collapsed {
    margin-bottom: 10px;
    margin-left: 5px;
    margin-right: 5px;
    margin-top: 10px;
  }

  .hide-show-button-expanded {
    margin-bottom: 10px;
    margin-left: 5px;
    margin-right: 5px;
  }

  .sidebar-card-expanded {
    position: absolute;
    top: 75px;
    right: 5px;
    bottom: 10px;
    z-index: 5;
    padding: 6px;
    border-radius: 30px !important;
  }

  .sidebar-card-collapsed {
    position: absolute;
    top: 75px;
    right: 5px;
    z-index: 5;
    padding: 6px;
    border-radius: 30px !important;
  }
</style>
