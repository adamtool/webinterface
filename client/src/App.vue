<template>
  <v-app absolute id='app'>
    <link href='https://fonts.googleapis.com/css?family=Roboto:300,400,500,700|Material+Icons'
          rel="stylesheet">
    <!--<v-toolbar</v-toolbar-items>-->
    <!--<v-spacer/>-->
    <!--<v-toolbar-title>Adam Frontend</v-toolbar-title>-->
    <!--</v-toolbar>-->
    <my-theme style="z-index: 999">
      <hsc-menu-bar :style="menuBarStyle" ref="menubar">
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

    <div style="display: flex; flex-direction: column; width: 100%; height: 95vh">
      <div style="display: flex; flex-direction: row; flex: 1 1 100%">
        <div class="flex-column-divider"
             v-on:click="isAptEditorVisible = !isAptEditorVisible">
          <div :class="isAptEditorVisible ? 'arrow-left' : 'arrow-right'"></div>
        </div>

        <v-flex xs6 md3 v-if="isAptEditorVisible">
          <div style="display: flex; flex-direction: column; height: 100%">
            <div style="text-align: center; flex: 0 0 58px; line-height: 58px; font-size: 18pt;">
              APT Editor
            </div>
            <textarea class='apt-text-area' style="flex: 1 1 100%" v-model='apt'/>
          </div>
        </v-flex>
        <v-flex xs12 id="graphEditorTabsFlex">
          <tabs>
            <tab name="Petri Game">
              <GraphEditor :graph='petriGame.net'
                           :dimensions='graphEditorDimensions'
                           v-on:graphModified='onGraphModified'
                           v-on:saveGraphAsAPT='savePetriGameAsAPT'
                           :shouldShowPhysicsControls="true"
                           :repulsionStrengthDefault="360"
                           :linkStrengthDefault="0.086"
                           :shouldShowSaveAPTButton="true"/>
            </tab>
            <tab name="Strategy BDD" v-if="strategyBDD"
                 :suffix="petriGame.uuid === strategyBDD.uuid ? '' : '****'">
              <GraphEditor :graph='strategyBDD'
                           :dimensions='graphEditorDimensions'/>
            </tab>
            <tab name="Graph Strategy BDD" v-if="graphStrategyBDD"
                 :suffix="petriGame.uuid === graphStrategyBDD.uuid ? '' : '****'">
              <GraphEditor :graph='graphStrategyBDD'
                           :dimensions='graphEditorDimensions'/>
            </tab>
            <tab name="Graph Game BDD" v-if="graphGameBDD"
                 :suffix="petriGame.uuid === graphGameBDD.uuid ? '' : '****'">
              <GraphEditor :graph='graphGameBDD'
                           :dimensions='graphEditorDimensions'
                           v-on:toggleStatePostset='toggleGraphGameStatePostset'
                           v-on:toggleStatePreset='toggleGraphGameStatePreset'
                           :shouldShowPhysicsControls="true"
                           :repulsionStrengthDefault="415"
                           :linkStrengthDefault="0.04"
                           :gravityStrengthDefault="300"/>
            </tab>
          </tabs>
        </v-flex>
      </div>
      <!--End first row-->
      <div style="flex: 1 1 100%">
        <LogViewer :messages="messageLog"
                   v-if="isLogVisible"/>
      </div>
      <div class="row-divider"
           style="flex: 0 0 48px;"
           v-on:click="isLogVisible = !isLogVisible">
        <div class="text">
          <template v-if="isLogVisible">Collapse Log</template>
          <template v-else>Show Log</template>
        </div>
        <div :class="isLogVisible ? 'arrow-down' : 'arrow-up'"></div>
      </div>
    </div>
    <v-layout>
      <v-flex xs12>
      </v-flex>
    </v-layout>
  </v-app>
</template>


<script>
  import aptFileTree from '@/aptExamples'
  import AptEditor from '@/components/AptEditor'
  import GraphEditor from '@/components/GraphEditor'
  import LogViewer from '@/components/LogViewer'
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
  import aptExample from './verySmallExample.apt'
  import HscMenuBarDirectory from './components/hsc-menu-bar-directory'

  import makeWebSocket from '@/logWebSocket'

  const ResizeSensor = require('css-element-queries/src/ResizeSensor')

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
      'my-theme': MyVueMenuTheme,
      'LogViewer': LogViewer
    },
    created: function () {
      // Connect to the server and subscribe to ADAM's log output
      // TODO Capture the error and display it in the log on screen if the websocket fails to open
      const socket = makeWebSocket(this.webSocketUrl)
      socket.$on('message', message => this.messageLog.push({
        source: 'server',
        level: '2', // TODO Handle levels (Warning, Error, Verbose, ...)
        time: new Date(),
        text: message
      }))
    },
    mounted: function () {
      this.parseAPTToPetriGame(this.apt)
      const flexElem = document.getElementById('graphEditorTabsFlex')
      const updateGraphEditorDimensions = () => {
        const width = flexElem.clientWidth
        const height = flexElem.clientHeight
        console.log('flex element changed to ' + width + ' x ' + height)
        this.graphEditorDimensions = {
          width: width,
          height: height
        }
      }

      // eslint-disable-next-line no-new
      new ResizeSensor(flexElem, updateGraphEditorDimensions)
      Vue.nextTick(updateGraphEditorDimensions) // Get correct dimensions after flexbox is rendered
    },
    data: function () {
      return {
        apt: aptExample,
        petriGame: {
          net: {
            links: [],
            nodes: []
          },
          uuid: 'abcfakeuuid123'
        },
        strategyBDD: null,
        graphStrategyBDD: null,
        graphGameBDD: null,
        isAptEditorVisible: true,
        isLogVisible: false,
        messageLog: [],
        graphEditorDimensions: {
          width: 0,
          height: 0
        }
      }
    },
    watch: {
      petriGame: function () {
        this.switchToPetriGameTab()
      },
      apt: function (apt) {
        this.parseAPTToPetriGame(this.apt)
      }
    },
    computed: {
      menuBarStyle: function () {
        const vuetifySidebarPadding = this.$vuetify.application.left
        return `border-radius: 0 0 4pt 0; padding-left: ${vuetifySidebarPadding + 10}px`
      },
      aptFileTree: function () {
        return aptFileTree
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
      },
      webSocketUrl: function () {
        if (this.baseUrl === '') { // This means that we are running in production, with relative URLs
          const loc = window.location
          let newUri
          if (loc.protocol === 'https:') {
            newUri = 'wss:'
          } else if (loc.protocol === 'http:') {
            newUri = 'ws:'
          } else {
            throw new Error('Error constructing the URL to use for our websocket connection.  ' +
              'Couldn\'t recognize the protocol string in window.location: ' + window.location)
          }
          return `${newUri}//${loc.host}/log`
        } else { // We are running in development mode with a hard-coded URL.  See main-dev.js
          return this.baseUrl.replace('http:', 'ws:').replace('https:', 'ws:') + '/log'
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
        this.log('Sending APT source code to backend.')
        axios.post(this.restEndpoints.convertAptToGraph, {
          params: {
            apt: apt
          }
        }).then(response => {
          switch (response.data.status) {
            case 'success':
              this.log('Successfully parsed APT. Received Petri Game from backend.')
              this.logVerbose(response)
              this.petriGame = response.data.graph
              break
            case 'error':
              this.log(`There was an error when we tried to parse the APT: ${response.data.message}`)
              break
            default:
              this.log('We got an unexpected response from the server when trying to parse the APT:')
              this.log(response)
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
        this.logVerbose('App: Received graphModified event from graph editor:')
        this.logVerbose(graph)
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
      },
      log: function (message, level) {
        this.messageLog.push({
          source: 'client',
          level: level === undefined ? 2 : level, // TODO handle log levels
          time: new Date(),
          text: message
        })
      },
      logVerbose: function (message) {
        this.log(message, 1)
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
    height: 100vh;
  }

  .apt-text-area {
    background: white;
    box-sizing: border-box;
    width: 100%;
    padding-left: 10px;
    resize: none;
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

  .arrow-up {
    width: 0;
    height: 0;
    border-left: 5px solid transparent;
    border-right: 5px solid transparent;
    border-bottom: 5px solid black;
  }

  .arrow-down {
    width: 0;
    height: 0;
    border-left: 20px solid transparent;
    border-right: 20px solid transparent;
    border-top: 20px solid black;
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
    margin-top: 59px;
  }

  .row-divider {
    display: flex;
    height: 48px;
    width: 100%;
    justify-content: center;
    align-items: center;
    border: 1px solid lightgray;
  }

  .row-divider:hover {
    border: 1px solid black;
  }

  .flex-column-divider:hover {
    transition: .2s;
    border: 1px solid black;
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
