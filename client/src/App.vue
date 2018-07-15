<template>
  <v-app absolute id='app'>
    <v-snackbar
      :timeout="6000"
      top
      multi-line
      :color="snackbarMessage.color"
      v-model="snackbarMessage.display">
      <div style="white-space: pre-wrap; font-size: 18px">
        {{ snackbarMessage.text }}
      </div>
      <v-btn flat @click.native="snackbarMessage.display = false">Close</v-btn>
    </v-snackbar>
    <input id="file-picker" type="file" style="display: none;" v-on:change="onFileSelected"/>
    <link href='https://fonts.googleapis.com/css?family=Roboto:300,400,500,700|Material+Icons'
          rel="stylesheet">
    <!--<v-toolbar</v-toolbar-items>-->
    <!--<v-spacer/>-->
    <!--<v-toolbar-title>Adam Frontend</v-toolbar-title>-->
    <!--</v-toolbar>-->
    <my-theme style="z-index: 999">
      <hsc-menu-bar :style="menuBarStyle" ref="menubar">
        <hsc-menu-bar-item label="File">
          <!--Have to use click.native so that the popup blocker isn't triggered-->
          <hsc-menu-item label="Load APT from file" @click.native="loadAptFromFile"/>
          <hsc-menu-item label="Save APT to file" @click="saveAptToFile"/>
          <hsc-menu-item label="Save Petri Game SVG to file" @click="saveSvgToFilePetriGame"/>
          <hsc-menu-item label="Save Strategy BDD SVG to file" @click="saveSvgToFileStrategyBDD"
                         v-if="strategyBDD"/>
          <hsc-menu-item label="Save Graph Strategy BDD SVG to file"
                         @click="saveSvgToFileGraphStrategyBDD" v-if="graphStrategyBDD"/>
          <hsc-menu-item label="Save Graph Game BDD SVG to file" @click="saveSvgToFileGraphGameBDD"
                         v-if="graphGameBDD"/>
          <hsc-menu-item label="Load example">
            <hsc-menu-bar-directory :fileTreeNode="aptFileTree"
                                    :callback="onAptExampleSelected"/>
          </hsc-menu-item>
        </hsc-menu-bar-item>
        <hsc-menu-bar-item @click.native="getStrategyBDD" label="Solve"/>
        <hsc-menu-bar-item label="Analyze">
          <hsc-menu-item @click.native="existsWinningStrategy"
                         label="Exists Winning Strategy?"/>
          <hsc-menu-item @click.native="getGraphStrategyBDD"
                         label="Get Graph Strategy BDD"/>
          <hsc-menu-item @click.native="getGraphGameBDD" label="Get Graph Game BDD"/>
        </hsc-menu-bar-item>
        <!--TODO Grey out these buttons or something if these things have already been calculated.-->
        <!--TODO Maybe add a little indicator for each one: "not yet calculated", "in progress", "Finished"-->
        <!--TODO For "existsWinningStrategy," it could even say whether or not a strategy exists.-->
      </hsc-menu-bar>
    </my-theme>

    <div style="width: 100%; height: 100vh">
      <div style="display: flex; flex-direction: row;" ref="horizontalSplitDiv">
        <div class="flex-column-divider"
             v-on:click="toggleAptEditor">
          <div :class="isAptEditorVisible ? 'arrow-left' : 'arrow-right'"></div>
        </div>

        <div id="aptEditorContainer" :style="aptEditorStyle">
          <div style="display: flex; flex-direction: column; height: 100%">
            <div style="text-align: center; flex: 0 0 58px; line-height: 58px; font-size: 18pt;">
              APT Editor
            </div>
            <textarea class='apt-text-area' style="flex: 1 1 100%" v-model='apt'/>
          </div>
        </div>
        <div id="graphEditorContainer">
          <tabs>
            <tab name="Petri Game" :style="petriGameTabStyle">
              <GraphEditor :graph='petriGame.net'
                           :dimensions='graphEditorDimensions'
                           ref='graphEditorPetriGame'
                           v-on:graphModified='onGraphModified'
                           v-on:saveGraphAsAPT='savePetriGameAsAPT'
                           v-on:insertNode='insertNode'
                           :shouldShowPhysicsControls="true"
                           :repulsionStrengthDefault="360"
                           :linkStrengthDefault="0.086"
                           :shouldShowSaveAPTButton="true"/>
            </tab>
            <tab name="Strategy BDD" v-if="strategyBDD"
                 :suffix="petriGame.uuid === strategyBDD.uuid ? '' : '****'">
              <GraphEditor :graph='strategyBDD'
                           ref='graphEditorStrategyBDD'
                           shouldShowPhysicsControls
                           :dimensions='graphEditorDimensions'/>
            </tab>
            <tab name="Graph Strategy BDD" v-if="graphStrategyBDD"
                 :suffix="petriGame.uuid === graphStrategyBDD.uuid ? '' : '****'">
              <GraphEditor :graph='graphStrategyBDD'
                           ref='graphEditorGraphStrategyBDD'
                           shouldShowPhysicsControls
                           :dimensions='graphEditorDimensions'/>
            </tab>
            <tab name="Graph Game BDD" v-if="graphGameBDD"
                 :suffix="petriGame.uuid === graphGameBDD.uuid ? '' : '****'">
              <GraphEditor :graph='graphGameBDD'
                           ref='graphEditorGraphGameBDD'
                           :dimensions='graphEditorDimensions'
                           v-on:toggleStatePostset='toggleGraphGameStatePostset'
                           v-on:toggleStatePreset='toggleGraphGameStatePreset'
                           :shouldShowPhysicsControls="true"
                           :repulsionStrengthDefault="415"
                           :linkStrengthDefault="0.04"
                           :gravityStrengthDefault="300"/>
            </tab>
          </tabs>
        </div>
      </div>
      <!--End first row-->
      <div ref="messageLogDiv">
        <LogViewer :messages="messageLog"/>
      </div>
    </div>
  </v-app>
</template>


<script>
  import aptFileTree from '@/aptExamples'
  import GraphEditor from '@/components/GraphEditor'
  import LogViewer from '@/components/LogViewer'
  import Vue from 'vue'
  import BootstrapVue from 'bootstrap-vue'
  import * as axios from 'axios'
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
  import { saveFileAs } from './fileutilities'

  const ResizeSensor = require('css-element-queries/src/ResizeSensor')

  import Split from 'split.js'

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
      'GraphEditor': GraphEditor,
      'my-theme': MyVueMenuTheme,
      'LogViewer': LogViewer
    },
    created: function () {
      // Connect to the server and subscribe to ADAM's log output
      // TODO Capture the error and display it in the log on screen if the websocket fails to open
      let socket
      try {
        socket = makeWebSocket(this.webSocketUrl)
      } catch (exception) {
        this.logError('An exception was thrown when opening the websocket connection to the server.  ' +
          'Server log messages may not be displayed.  Exception:')
        this.logError(exception.message)
        return
      }
      socket.$on('message', message => {
        const messageParsed = JSON.parse(message)
        const logEntry = {
          source: 'server',
          level: messageParsed.level,
          time: new Date(),
          text: messageParsed.message
        }
        this.messageLog.push(logEntry)
      })
      socket.$on('error', () => {
        this.logError('The websocket connection to the server threw an error.  ADAM\'s log output might not ' +
          'be displayed.')
      })
    },
    mounted: function () {
      this.parseAPTToPetriGame(this.apt)
      const flexElem = document.getElementById('graphEditorContainer')
      const updateGraphEditorDimensions = () => {
        const width = flexElem.clientWidth
        const height = flexElem.clientHeight
        // this.logVerbose('flex element changed to ' + width + ' x ' + height)
        this.graphEditorDimensions = {
          width: width,
          height: height
        }
      }

      // eslint-disable-next-line no-new
      new ResizeSensor(flexElem, updateGraphEditorDimensions)
      Vue.nextTick(updateGraphEditorDimensions) // Get correct dimensions after flexbox is rendered
      this.log('Hello!')

      // Initialize draggable, resizable panes for APT editor and log viewer
      this.aptEditorSplit = this.createAptEditorSplit()
      this.createVerticalSplit()
    },
    data: function () {
      return {
        apt: aptExample,
        aptParseStatus: 'success',
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
        messageLog: [],
        graphEditorDimensions: {
          width: 0,
          height: 0
        },
        snackbarMessage: {
          display: false,
          text: '',
          color: undefined
        },
        aptEditorSplit: undefined,  // See "API" section on https://nathancahill.github.io/Split.js/
        aptEditorSplitSizes: [25, 75],
        aptEditorMinWidth: 7.65 // Percentage of flexbox container's width
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
      // TODO figure out why this doesn't work
      petriGameTabStyle: function () {
        switch (this.petriGame.hasWinningStrategy) {
          case undefined:
            return ''
          case true:
            return 'background: lightgreen'
          case false:
            return 'background: lightred'
          default:
            return ''
        }
      },
      menuBarStyle: function () {
        const vuetifySidebarPadding = this.$vuetify.application.left
        return `border-radius: 0 0 4pt 0; padding-left: ${vuetifySidebarPadding + 10}px`
      },
      aptFileTree: function () {
        return aptFileTree
      },
      aptEditorStyle: function () {
        const hideStyle = this.isAptEditorVisible ? '' : 'display: none;'
        let color
        switch (this.aptParseStatus) {
          case 'success':
            color = '#5959ed'
            break
          case 'error':
            color = 'red'
            break
          case 'running':
            color = 'lightgray'
            break
          default:
            this.showErrorNotification('Got an invalid value for aptParseStatus: ' + this.aptParseStatus)
        }
        const borderStyle = `border: 3px solid ${color};`
        return hideStyle + borderStyle
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
          convertAptToGraph: this.baseUrl + '/convertAptToGraph',
          insertNode: this.baseUrl + '/insertPlace'
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
            const errorMessage = 'Error constructing the URL to use for our websocket connection.  ' +
              'Couldn\'t recognize the protocol string in window.location: ' + window.location
            this.logError(errorMessage)
            throw new Error(errorMessage)
          }
          return `${newUri}//${loc.host}/log`
        } else { // We are running in development mode with a hard-coded URL.  See main-dev.js
          return this.baseUrl.replace('http:', 'ws:').replace('https:', 'ws:') + '/log'
        }
      }
    },
    methods: {
      createVerticalSplit: function () {
        const split = Split([this.$refs.horizontalSplitDiv, this.$refs.messageLogDiv], {
          minSize: 0,
          gutterSize: 20,
          direction: 'vertical',
          snapOffset: 50,
          sizes: [80, 20]
        })
        return split
      },
      createAptEditorSplit: function () {
        const split = Split(['#aptEditorContainer', '#graphEditorContainer'], {
          sizes: this.aptEditorSplitSizes,
          minSize: 0,
          gutterSize: 20,
          elementStyle: function (dimension, size, gutterSize) {
            return {
              'flex-basis': `calc(${size}% - ${gutterSize}px)`
            }
          },
          gutterStyle: function (dimension, gutterSize) {
            return {
              'flex-basis': gutterSize + 'px'
            }
          },
          onDrag: () => {
            const aptEditorWidth = split.getSizes()[0]
            const shouldAptEditorCollapse = aptEditorWidth <= this.aptEditorMinWidth
            if (shouldAptEditorCollapse && this.isAptEditorVisible) {
              this.logVerbose('collapsing apt editor')
              this.isAptEditorVisible = false
            }
            if (shouldAptEditorCollapse) {
              split.setSizes([0, 100])
            }
            if (!shouldAptEditorCollapse && !this.isAptEditorVisible) {
              this.logVerbose('expanding apt editor')
              this.isAptEditorVisible = true
            }
          },
          onDragEnd: () => {
            if (this.isAptEditorVisible) {
              // Save current sizes so that we can restore them in toggleAptEditor()
              this.aptEditorSplitSizes = split.getSizes()
            }
          }
        })
        return split
      },
      // Hide or show APT editor, restoring its size if appropriate
      toggleAptEditor: function () {
        this.logVerbose('toggleAptEditor()')
        const restoring = !this.isAptEditorVisible
        if (restoring) {
          this.aptEditorSplit.setSizes(this.aptEditorSplitSizes)
        } else {
          this.aptEditorSplit.setSizes([0, 100])
        }
        this.isAptEditorVisible = !this.isAptEditorVisible
      },
      // Load APT from a text file stored on the user's local filesystem
      // See https://developer.mozilla.org/en-US/docs/Web/API/File/Using_files_from_web_applications
      onFileSelected: function (changeEvent) {
        this.logVerbose('The user selected a file in the file selector')
        const file = changeEvent.target.files[0]
        this.logObject(file)
        const reader = new FileReader()
        reader.onloadend = () => {
          // TODO verify that the file is reasonable (i.e. plain text, not a binary or other weird file)
          this.logVerbose('The file selected by the user is finished loading.  Updating text editor contents')
          this.onAptExampleSelected(reader.result)
        }
        reader.readAsText(file)
      },
      loadAptFromFile: function () {
        document.getElementById('file-picker').click()
      },
      saveAptToFile: function () {
        saveFileAs(this.apt, 'apt.txt')
      },
      saveSvgToFilePetriGame: function () {
        this.$refs.graphEditorPetriGame.saveGraph()
      },
      saveSvgToFileStrategyBDD: function () {
        this.$refs.graphEditorStrategyBDD.saveGraph()
      },
      saveSvgToFileGraphStrategyBDD: function () {
        this.$refs.graphEditorGraphStrategyBDD.saveGraph()
      },
      saveSvgToFileGraphGameBDD: function () {
        this.$refs.graphEditorGraphGameBDD.saveGraph()
      },
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
        this.logVerbose('Sending APT source code to backend.')
        axios.post(this.restEndpoints.convertAptToGraph, {
          params: {
            apt: apt
          }
        }).then(response => {
          switch (response.data.status) {
            case 'success':
              this.logVerbose('Successfully parsed APT. Received Petri Game from backend.')
              this.logObject(response)
              this.petriGame = response.data.graph
              this.aptParseStatus = 'success'
              break
            case 'error':
              this.log(`There was an error when we tried to parse the APT: ${response.data.message}`)
              this.aptParseStatus = 'error'
              break
            default:
              this.log('We got an unexpected response from the server when trying to parse the APT:')
              this.log(response)
              this.aptParseStatus = 'error'
              break
          }
        }).catch(() => {
          this.logError('Network error when trying to parse APT')
        })
        this.aptParseStatus = 'running'
      }, 200),
      existsWinningStrategy: function () {
        if (this.petriGame.hasWinningStrategy !== undefined) {
          // TODO maintain state, avoid unnecessarily calculating this multiple times
          // throw new Error('Winning strategy has already been calculated for this Petri Game')
        }
        axios.post(this.restEndpoints.existsWinningStrategy, {
          petriGameId: this.petriGame.uuid
        }).then(response => {
          this.withErrorHandling(response, response => {
            this.petriGame.hasWinningStrategy = response.data.result
            // TODO consider displaying the info in a more persistent way, e.g. by colorizing the button "exists winning strategy".
            if (response.data.result) {
              this.showSuccessNotification('Yes, there is a winning strategy for this Petri Game.')
            } else {
              this.showErrorNotification('No, there is no winning strategy for this Petri Game.')
            }
          })
        }).catch(() => {
          this.logError('Network error in existsWinningStrategy')
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
        }).catch(() => {
          this.logError('Network error in getStrategyBDD')
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
        }).catch(() => {
          this.logError('Network error in getGraphStrategyBDD')
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
        }).catch(() => {
          this.logError('Network error in getGraphGameBDD')
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
        }).catch(() => {
          this.logError('Network error')
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
        }).catch(() => {
          this.logError('Network error')
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
        }).catch(() => {
          this.logError('Network error')
        })
      },
      insertNode: function (nodeSpec) {
        console.log('processing insertNode event')
        axios.post(this.restEndpoints.insertNode, {
          petriGameId: this.petriGame.uuid,
          nodeType: nodeSpec.type,
          x: nodeSpec.x,
          y: nodeSpec.y
        }).then(response => {
          this.withErrorHandling(response, response => {
            this.petriGame.net = response.data.result
          })
        }).catch(() => {
          this.logError('Network error')
        })
      },
      onGraphModified: function (graph) {
        this.logVerbose('App: Received graphModified event from graph editor:')
        this.logObject(graph)
        // TODO: Implement undo/redo.
      },
      onAptExampleSelected: function (apt) {
        // Let the Graph Editor know that a new petri game has been loaded.
        // This needs to be handled differently than an incremental edit to an already loaded
        // Petri Game, because when we load a new APT file, we want all of the nodes' positions
        // to be reset.
        this.$refs.graphEditorPetriGame.onLoadNewPetriGame()
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
        this.logError(message)
        this.showNotification(message, 'pink')
      },
      showSuccessNotification (message) {
        this.log(message)
        this.showNotification(message, 'green')
      },
      showNotification: function (message, color) {
        this.snackbarMessage = {
          display: true,
          color: color,
          text: message
        }
      },
      log: function (message, level) {
        this.messageLog.push({
          source: 'client',
          level: level === undefined ? 2 : level, // TODO handle log levels
          time: new Date(),
          text: message
        })
      },
      logObject: function (message) {
        this.log(message, 0)
      },
      logVerbose: function (message) {
        this.log(message, 1)
      },
      logError: function (message) {
        this.log(message, 4)
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
    font-size: 18px;
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
    border-left: 10px solid transparent;
    border-right: 10px solid transparent;
    border-bottom: 10px solid black;
  }

  .arrow-down {
    width: 0;
    height: 0;
    border-left: 10px solid transparent;
    border-right: 10px solid transparent;
    border-top: 10px solid black;
  }

  .flex-column-divider {
    flex: 0 0 45px;
    display: flex;
    justify-content: center;
    align-items: center;
    border: 1px solid lightgray;
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

  .gutter {
    background-color: #eee;
    background-repeat: no-repeat;
    background-position: 50%;
  }

  .gutter.gutter-horizontal {
    background-image: url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUAAAAeCAYAAADkftS9AAAAIklEQVQoU2M4c+bMfxAGAgYYmwGrIIiDjrELjpo5aiZeMwF+yNnOs5KSvgAAAABJRU5ErkJggg==');
    cursor: ew-resize;
    margin-left: 5px;
    margin-right: 5px;
  }

  .gutter.gutter-vertical {
    background-image: url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAB4AAAAFAQMAAABo7865AAAABlBMVEVHcEzMzMzyAv2sAAAAAXRSTlMAQObYZgAAABBJREFUeF5jOAMEEAIEEFwAn3kMwcB6I2AAAAAASUVORK5CYII=');
    margin-top: 5px;
    margin-bottom: 5px;
    cursor: ns-resize;
  }
</style>
