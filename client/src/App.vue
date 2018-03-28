<template>
  <div id='app'>
    <div style="margin-bottom: 10px; margin-left: 10px;">
      <AptExamplePicker v-on:fileSelected='onAptExampleSelected'/>
      <!--Toolbar row-->
      <!--TODO Grey out these buttons or something if these things have already been calculated.-->
      <!--TODO Maybe add a little indicator for each one: "not yet calculated", "in progress", "Finished"-->
      <!--TODO For "existsWinningStrategy," it could even say whether or not a strategy exists.-->
      <button type="button" class="btn btn-primary" v-on:click="existsWinningStrategy">
        Exists winning strategy?
      </button>
      <button type="button" class="btn btn-primary" v-on:click="getStrategyBDD">
        Get Strategy BDD
      </button>
      <button type="button" class="btn btn-primary" v-on:click="getGraphStrategyBDD">
        Get Graph Strategy BDD
      </button>
      <button type="button" class="btn btn-primary" v-on:click="getGraphGameBDD">
        Get Graph Game BDD
      </button>
      <!--End toolbar row-->
    </div>

    <!--Main flexbox container-->
    <div style="display: flex; flex-direction: row; margin-top: 5px;">
      <div :style="aptEditorStyle">
        <div style="text-align: center; line-height: 58px; height: 58px; font-size: 18pt;">
          APT Editor
        </div>
        <AptEditor :apt='apt'
                   v-on:textEdited='parseAPTToPetriGame'></AptEditor>
      </div>
      <div class="flex-column-divider"
           v-on:click="isAptEditorVisible = !isAptEditorVisible">
        <div class="text">
          <template v-if="isAptEditorVisible">Collapse APT editor</template>
          <template v-else>Show APT editor</template>
        </div>
        <div :class="isAptEditorVisible ? 'arrow-left' : 'arrow-right'"></div>
      </div>
      <div class="tab-container" style="flex: 1 1 100%">
        <tabs>
          <tab name="Petri Game">
            <template v-if="!aptParsingResult">
              <!--The Petri Game will appear here after you type in some APT.-->
            </template>
            <template v-else-if="aptParsingResult.status === 'success'">
              <GraphEditor :graph='petriGame.net'
                           v-on:graphModified='onGraphModified'
                           v-on:saveGraphAsAPT='savePetriGameAsAPT'
                           :shouldShowPhysicsControls="true"
                           :repulsionStrengthDefault="360"
                           :linkStrengthDefault="0.086"
                           :shouldShowSaveAPTButton="true"/>
            </template>
            <template v-else-if="aptParsingResult.status === 'error'">
              There was an error when we tried to parse the APT:
              <pre>{{ aptParsingResult.message }}</pre>
            </template>
            <template v-else>
              We got an unexpected response from the server when trying to parse the APT:
              <pre>{{ aptParsingResult }}</pre>
            </template>
          </tab>
          <tab name="Strategy BDD" v-if="strategyBDD"
               :suffix="petriGame.uuid === strategyBDD.uuid ? '' : '****'">
            <GraphEditor :graph='strategyBDD'></GraphEditor>
          </tab>
          <tab name="Graph Strategy BDD" v-if="graphStrategyBDD"
               :suffix="petriGame.uuid === graphStrategyBDD.uuid ? '' : '****'">
            <GraphEditor :graph='graphStrategyBDD'></GraphEditor>
          </tab>
          <tab name="Graph Game BDD" v-if="graphGameBDD"
               :suffix="petriGame.uuid === graphGameBDD.uuid ? '' : '****'">
            <GraphEditor :graph='graphGameBDD'
                         v-on:toggleStatePostset='toggleGraphGameStatePostset'
                         v-on:toggleStatePreset='toggleGraphGameStatePreset'
                         :shouldShowPhysicsControls="true"
                         :repulsionStrengthDefault="415"
                         :linkStrengthDefault="0.04"
                         :gravityStrengthDefault="300"></GraphEditor>
          </tab>
        </tabs>
      </div>
    </div>
    <!--End main flexbox container-->

  </div>
</template>

<script>
  import AptExamplePicker from '@/components/AptExamplePicker'
  import AptEditor from '@/components/AptEditor'
  import GraphEditor from '@/components/GraphEditor'
  import Vue from 'vue'
  import BootstrapVue from 'bootstrap-vue'
  import * as axios from 'axios'
  import * as iziToast from 'izitoast'
  import 'izitoast/dist/css/iziToast.min.css'
  import { Tabs, Tab } from 'vue-tabs-component'
  import './tabs-component.css'
  import {debounce} from 'underscore'

  Vue.component('tabs', Tabs)
  Vue.component('tab', Tab)

  Vue.use(BootstrapVue)
  import 'bootstrap/dist/css/bootstrap.css'
  import 'bootstrap-vue/dist/bootstrap-vue.css'
  import aptExample from './mutex.apt'

  export default {
    name: 'app',
    props: {
      baseUrl: {
        type: String,
        default: ''
      }
    },
    components: {
      'AptEditor': AptEditor,
      'GraphEditor': GraphEditor,
      'AptExamplePicker': AptExamplePicker
    },
    mounted: function () {
      this.parseAPTToPetriGame(this.apt)
    },
    data: function () {
      return {
        apt: aptExample,
        aptParsingResult: {},
        strategyBDD: null,
        graphStrategyBDD: null,
        graphGameBDD: null,
        isAptEditorVisible: true
      }
    },
    watch: {
      petriGame: function () {
        this.switchToPetriGameTab()
      }
    },
    computed: {
      petriGame: function () {
        if (this.aptParsingResult.graph) {
          return this.aptParsingResult.graph
        } else {
          return ({
            net: {
              links: [],
              nodes: []
            },
            uuid: 'abcfakeuuid123'
          })
        }
      },
      aptEditorStyle: function () {
        if (this.isAptEditorVisible) {
          return 'flex: 1 1 1000px; margin-left: 15px;'
        } else {
          return 'display: none;'
        }
      },
      // Depending on whether we are in development mode or in production, the URLs used for server
      // requests are different.  Production uses relative urls, while dev mode uses hard-coded
      // "localhost:4567/..."
      restEndpoints: function () {
        return {
          existsWinningStrategy: this.baseUrl + '/existsWinningStrategy',
          getStrategyBDD: this.baseUrl + '/getStrategyBDD',
          getGraphStrategyBDD: this.baseUrl + '/getGraphStrategyBDD',
          getGraphGameBDD: this.baseUrl + '/getGraphGameBDD',
          toggleGraphGameBDDNodePostset: this.baseUrl + '/toggleGraphGameBDDNodePostset',
          toggleGraphGameBDDNodePreset: this.baseUrl + '/toggleGraphGameBDDNodePreset',
          savePetriGameAsAPT: this.baseUrl + '/savePetriGameAsAPT',
          convertAptToGraph: this.baseUrl + '/convertAptToGraph'
        }
      }
    },
    methods: {
      switchToPetriGameTab: function () {
        window.location.href = '#petri-game'
      },
      switchToStrategyBDDTab: function () {
        window.location.href = '#strategy-bdd'
      },
      switchToGraphStrategyBDDTab: function () {
        window.location.href = '#graph-strategy-bdd'
      },
      switchToGraphGameBDDTab: function () {
        window.location.href = '#graph-game-bdd'
      },
      // Send APT to backend and parse it, then display the resulting Petri Game.
      // This is debounced using Underscore: http://underscorejs.org/#debounce
      parseAPTToPetriGame: debounce(function (apt) {
        this.switchToPetriGameTab()
        console.log('Sending APT source code to backend.')
        axios.post(this.restEndpoints.convertAptToGraph, {
          params: {
            apt: apt
          }
        }).then(response => {
          switch (response.data.status) {
            case 'success':
              console.log('Received graph from backend:')
              console.log(response.data)
              this.aptParsingResult = response.data
              break
            default:
              console.log('response.data.status was not success :(')
              this.aptParsingResult = response.data
              break
          }
          // TODO handle broken connection to server
        })
      }, 200),
      existsWinningStrategy: function () {
        axios.post(this.restEndpoints.existsWinningStrategy, {
          petriGameId: this.petriGame.uuid
        }).then(response => {
          this.withErrorHandling(response, response => {
            // TODO consider displaying the info in a more persistent way, e.g. by colorizing the button "exists winning strategy".
            // This is another piece of state that maybe should be kept on the server.
            if (response.data.result) {
              this.showSuccessNotification('Yes, there is a winning strategy for this Petri Game.')
            } else {
              this.showErrorNotification('No, there is no winning strategy for this Petri Game.')
            }
          })
        })
      },
      getStrategyBDD: function () {
        const uuid = this.petriGame.uuid
        axios.post(this.restEndpoints.getStrategyBDD, {
          petriGameId: uuid
        }).then(response => {
          this.withErrorHandling(response, response => {
            this.strategyBDD = response.data.strategyBDD
            this.strategyBDD.uuid = uuid
            this.switchToStrategyBDDTab()
          })
        })
      },
      getGraphStrategyBDD: function () {
        const uuid = this.petriGame.uuid
        axios.post(this.restEndpoints.getGraphStrategyBDD, {
          petriGameId: uuid
        }).then(response => {
          this.withErrorHandling(response, response => {
            this.graphStrategyBDD = response.data.graphStrategyBDD
            this.graphStrategyBDD.uuid = uuid
            this.switchToGraphStrategyBDDTab()
          })
        })
      },
      getGraphGameBDD: function () {
        const uuid = this.petriGame.uuid
        axios.post(this.restEndpoints.getGraphGameBDD, {
          petriGameId: uuid
        }).then(response => {
          this.withErrorHandling(response, response => {
            this.graphGameBDD = response.data.graphGameBDD
            this.graphGameBDD.uuid = uuid
            this.switchToGraphGameBDDTab()
          })
        })
      },
      toggleGraphGameStatePostset: function (stateId) {
        const uuid = this.petriGame.uuid
        axios.post(this.restEndpoints.toggleGraphGameBDDNodePostset, {
          petriGameId: uuid,
          stateId: stateId
        }).then(response => {
          this.withErrorHandling(response, response => {
            this.graphGameBDD = response.data.graphGameBDD
            this.graphGameBDD.uuid = uuid
          })
        })
      },
      toggleGraphGameStatePreset: function (stateId) {
        const uuid = this.petriGame.uuid
        axios.post(this.restEndpoints.toggleGraphGameBDDNodePreset, {
          petriGameId: uuid,
          stateId: stateId
        }).then(response => {
          this.withErrorHandling(response, response => {
            this.graphGameBDD = response.data.graphGameBDD
            this.graphGameBDD.uuid = uuid
          })
        })
      },
      // Our graph editor may give us an object with Node IDs as keys and x,y coordinates as values.
      // We send those x,y coordinates to the server and get back an APT with those coordinates in it.
      // We put that APT into the APT editor .
      savePetriGameAsAPT: function (mapNodeIDXY) {
        axios.post(this.restEndpoints.savePetriGameAsAPT, {
          petriGameId: this.petriGame.uuid,
          nodeXYCoordinateAnnotations: mapNodeIDXY
        }).then(response => {
          this.withErrorHandling(response, response => {
            this.apt = response.data.apt
            this.isAptEditorVisible = true
          })
        })
      },
      onGraphModified: function (graph) {
        console.log('App: Received graphModified event from graph editor:')
        console.log(graph)
        // TODO: Implement undo/redo.
      },
      onAptExampleSelected: function (apt) {
        this.apt = apt
        this.isAptEditorVisible = true
      },
      withErrorHandling: function (response, onSuccessCallback) {
        switch (response.data.status) {
          case 'success':
            onSuccessCallback(response)
            break
          case 'error':
            this.showErrorNotification(response.data.message)
            break
          default:
            this.showErrorNotification(`Received a malformed response from the server: ${response.data}`)
        }
      },
      showErrorNotification (message) {
        this.showNotification(message, 'red')
      },
      showSuccessNotification (message) {
        this.showNotification(message, 'green')
      },
      showNotification: function (message, color) {
        iziToast.show({
          color: color,
          timeout: 3000,
          message: message,
          position: 'bottomCenter',
          overlayClose: true,
          closeOnEscape: true
        })
      }
    }
  }
</script>

<style>
  #app {
    font-family: 'Avenir', Helvetica, Arial, sans-serif;
    -webkit-font-smoothing: antialiased;
    -moz-osx-font-smoothing: grayscale;
    color: #2c3e50;
    margin-top: 20px;
  }

  .action-buttons {
    /*text-align: center;*/
  }

  .iziToast > .iziToast-body {
    white-space: pre-wrap;
  }

  /*https://css-tricks.com/snippets/css/css-triangle/*/
  .arrow-left {
    width: 0;
    height: 0;
    border-top: 10px solid transparent;
    border-bottom: 10px solid transparent;
    border-right: 10px solid blue;
  }

  .arrow-right {
    width: 0;
    height: 0;
    border-top: 10px solid transparent;
    border-bottom: 10px solid transparent;
    border-left: 10px solid blue;
  }

  .flex-column-divider {
    flex: 0 0 45px;
    display: flex;
    justify-content: center;
    align-items: center;
    border: 1px solid lightgray;
    transition: .2s;
    margin-top: 59px;
  }

  .flex-column-divider:hover {
    transition: .2s;
    flex: 0 0 100px;
  }

  .flex-column-divider .text {
    margin: 0;
    white-space: pre;
    font-size: 0;
    transition: .2s;
  }

  .flex-column-divider:hover .text {
    margin: 5px;
    font-size: inherit;
    transition: .2s;
  }
</style>
