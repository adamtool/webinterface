<template>
  <div id='app'>
    <link href='https://fonts.googleapis.com/css?family=Roboto:300,400,500,700|Material+Icons'
          rel="stylesheet">
    <v-app absolute>
      <!--<v-toolbar app>-->
      <!--<v-toolbar-items>-->

      <!--</v-toolbar-items>-->
      <!--</v-toolbar>-->
      <v-toolbar app>
        <v-toolbar-items>
          <v-btn @click.stop="isAptEditorVisible = !isAptEditorVisible">Toggle APT Editor</v-btn>
          <v-menu :nudge-width="100">
            <v-btn slot="activator">
              All
              <v-icon dark>arrow_drop_down</v-icon>
            </v-btn>
            <v-list>
              <v-list-tile>
                <v-list-tile-content>
                  Apples
                </v-list-tile-content>
              </v-list-tile>
              <v-list-tile>
                <v-list-tile-title>
                  Bananas
                </v-list-tile-title>
              </v-list-tile>
            </v-list>
          </v-menu>
          <!--<v-menu>-->
          <!--<v-toolbar-title slot="activator">-->
          <!--<span>File</span>-->
          <!--<v-icon>arrow_drop_down</v-icon>-->
          <!--</v-toolbar-title>-->
          <!--<v-list>-->
          <!--<v-list-tile>-->
          <!--<v-list-tile-title v-text="Apples"></v-list-tile-title>-->
          <!--</v-list-tile>-->
          <!--<v-list-tile>-->
          <!--<v-list-tile-title v-text="Bananas"></v-list-tile-title>-->
          <!--</v-list-tile>-->
          <!--</v-list>-->
          <!--</v-menu>-->
          <my-theme>
            <hsc-menu-bar style="border-radius: 0 0 4pt 0" ref="menubar">
              <hsc-menu-bar-item label="File">
                <hsc-menu-item label="New"/>
              </hsc-menu-bar-item>
              <hsc-menu-bar-item label="Examples">
                <hsc-menu-bar-directory :fileTreeNode="aptFileTree"
                                        :callback="onAptExampleSelected"/>
              </hsc-menu-bar-item>
              <!--TODO Grey out these buttons or something if these things have already been calculated.-->
              <!--TODO Maybe add a little indicator for each one: "not yet calculated", "in progress", "Finished"-->
              <!--TODO For "existsWinningStrategy," it could even say whether or not a strategy exists.-->
              <hsc-menu-bar-item @click.native="existsWinningStrategy"
                                 label="Exists Winning Strategy?"/>
              <hsc-menu-bar-item @click.native="getStrategyBDD" label="Get Strategy BDD"/>
              <hsc-menu-bar-item @click.native="getGraphStrategyBDD"
                                 label="Get Graph Strategy BDD"/>
              <hsc-menu-bar-item @click.native="getGraphGameBDD" label="Get Graph Game BDD"/>
            </hsc-menu-bar>
          </my-theme>
        </v-toolbar-items>
        <v-spacer/>
        <v-toolbar-title>Adam Frontend</v-toolbar-title>
      </v-toolbar>
      <v-navigation-drawer v-model="isAptEditorVisible" absolute app clipped>
        <div style="text-align: center; line-height: 58px; height: 58px; font-size: 18pt;">
          APT Editor
        </div>
        <AptEditor :apt='apt'
                   v-on:textEdited='parseAPTToPetriGame'></AptEditor>
      </v-navigation-drawer>

      <v-content>
        <v-expansion-panel expand>
          <v-expansion-panel-content value="true">
            <v-card>
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
            </v-card>
          </v-expansion-panel-content>
          <v-expansion-panel-content>
            <div slot="header">Log</div>
            <!--log viewer-->
            <!--TODO Make it hide-able and look good-->
            <pre>{{ this.adamMessageLog }}</pre>
          </v-expansion-panel-content>
        </v-expansion-panel>
      </v-content>
    </v-app>
  </div>
</template>


<script>
  import aptFileTree from '@/aptExamples'
  import AptEditor from '@/components/AptEditor'
  import GraphEditor from '@/components/GraphEditor'
  import Vue from 'vue'
  import BootstrapVue from 'bootstrap-vue'
  import * as axios from 'axios'
  import * as iziToast from 'izitoast'
  import 'izitoast/dist/css/iziToast.min.css'
  import { Tabs, Tab } from 'vue-tabs-component'
  import './tabs-component.css'
  import { debounce } from 'underscore'
  import * as VueMenu from '@hscmap/vue-menu'
  import Vuetify from 'vuetify'

  Vue.use(Vuetify)
  import 'vuetify/dist/vuetify.min.css'

  Vue.use(VueMenu)
  import MyVueMenuTheme from '@/menuStyle'

  Vue.component('tabs', Tabs)
  Vue.component('tab', Tab)

  Vue.use(BootstrapVue)
  import 'bootstrap/dist/css/bootstrap.css'
  import 'bootstrap-vue/dist/bootstrap-vue.css'
  import aptExample from './mutex.apt'
  import HscMenuBarDirectory from './components/hsc-menu-bar-directory'

  import makeWebSocket from '@/logWebSocket'

  export default {
    name: 'app',
    props: {
      baseUrl: {
        type: String,
        default: ''
      }
    },
    components: {
      HscMenuBarDirectory, // TODO decide on import style
      'AptEditor': AptEditor,
      'GraphEditor': GraphEditor,
      'my-theme': MyVueMenuTheme
    },
    created: function () {
      // Connect to the server and subscribe to ADAM's log output
      // TODO!!! Stop hard-coding this URL
      const socket = makeWebSocket('ws://localhost:4567/log')
      socket.$on('message', message => (this.adamMessageLog = this.adamMessageLog + message))
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
        isAptEditorVisible: true,
        adamMessageLog: ''
      }
    },
    watch: {
      petriGame: function () {
        this.switchToPetriGameTab()
      }
    },
    computed: {
      aptFileTree: function () {
        console.log(aptFileTree)
        return aptFileTree
      },
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
              console.log('response.data.status indicates APT parsing failure.')
              this.aptParsingResult = response.data
              break
          }
          // TODO handle broken connection to server
        })
      }, 200),
      existsWinningStrategy: function () {
        this.$refs.menubar.deactivate()
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
        this.$refs.menubar.deactivate()
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
        this.$refs.menubar.deactivate()
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
        this.$refs.menubar.deactivate()
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

  /*TODO Make a version of this that works for vertical layout as well*/
  /*TODO Refactor so that this can be used as a component w/o making a giant single template*/
  /*i.e. <vsplit :proportion="0.4"> <vsplitchild></vsplitchild> <vsplitchild></vsplitchild></vsplit>*/
  /* Instead of having to manually create the flexbox and apply flex attributes to children */
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

  #footer {
    position: absolute;
    bottom: 30px;
    height: 300px;
    overflow: hidden;
    width: 100%;
  }
</style>
