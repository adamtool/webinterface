<template>
  <v-app absolute id='app'>
    <v-dialog v-model="showJobList"
              :hide-overlay="false"
              :persistent="false"
              @keydown.esc="showJobList = false">
      <v-card>
        <v-card-title
          primary-title
          style="justify-content: space-between;">
          <span>Your jobs run/running/queued on the server</span>
          <v-icon standard right
                  @click="showJobList = false">
            close
          </v-icon>
        </v-card-title>
        <v-card-text>
          <JobList
            :jobListings="availableBDDGraphListings"
            :useModelChecking="useModelChecking"
            style="background-color: white;"
            @getGraphGameBdd="getGraphGameBdd"
            @getWinningStrategy="getWinningStrategy"
            @getGraphStrategyBdd="getGraphStrategyBdd"
            @cancelJob="cancelJob"
            @deleteJob="deleteJob"/>
          <div
            style="padding-top: 15px;">
            <v-expansion-panel>
              <v-expansion-panel-content>
                <template v-slot:header>
                  <div>More info</div>
                </template>
                <v-card>
                  <v-card-text>
                    <div>
                      The jobs listed here are stored in-memory on the server and will disappear if
                      the server is restarted.
                    </div>
                    <div>
                      You will also lose access to them if you clear the "local
                      storage" of your browser. That's because you can only see jobs that correspond
                      to a randomly generated unique ID that is stored in your local storage.
                    </div>
                    <div>Your unique ID is {{ browserUuid }}.</div>
                    <div>
                      If you use multiple browsers, you can share one unique ID between them in
                      order to have the same list of jobs appear in all of your browsers.
                    </div>
                    <v-text-field
                      v-model="browserUuidEntry"
                      :rules="[validateBrowserUuid]"
                      label="Other Browser UUID"/>
                    <v-btn
                      @click="saveBrowserUuid">
                      Use other UUID
                    </v-btn>
                  </v-card-text>
                </v-card>
              </v-expansion-panel-content>
            </v-expansion-panel>
          </div>
        </v-card-text>
      </v-card>
    </v-dialog>
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
    <!--<v-toolbar</v-toolbar-items>-->
    <!--<v-spacer/>-->
    <!--<v-toolbar-title>Adam Frontend</v-toolbar-title>-->
    <!--</v-toolbar>-->
    <my-theme
      style="z-index: 999; display: flex; flex-direction: row; justify-content: space-between;">
      <hsc-menu-bar :style="menuBarStyle" ref="menubar">
        <hsc-menu-bar-item label="File">
          <!--Have to use click.native so that the popup blocker isn't triggered-->
          <hsc-menu-item label="Load APT from file" @click.native="loadAptFromFile"/>
          <v-dialog
            style="display: block;"
            v-model="showSaveAptModal"
            width="400"
            @keydown.esc="showSaveAptModal = false">
            <template v-slot:activator="{ on }">
              <hsc-menu-item label="Save APT to file"
                             @click="onClickSaveAptMenuItem"/>
            </template>
            <v-card>
              <v-card-title
                primary-title
                style="justify-content: space-between;">
                <span>Save APT</span>
                <v-icon standard right
                        @click="showSaveAptModal = false">
                  close
                </v-icon>
              </v-card-title>
              <v-card-text>
                <v-text-field
                  v-model="aptFilename"
                  label="Filename">
                </v-text-field>
                <v-btn @click="saveAptToFile(); showSaveAptModal = false">
                  Save APT
                </v-btn>
              </v-card-text>
            </v-card>
          </v-dialog>
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
        <template v-if="useDistributedSynthesis">
          <hsc-menu-bar-item @click.native="calculateStrategyBDD" label="Solve"/>
          <hsc-menu-bar-item label="Analyze">
            <hsc-menu-item @click.native="calculateExistsWinningStrategy"
                           label="Exists Winning Strategy?"/>
            <hsc-menu-item @click.native="calculateGraphStrategyBDD"
                           label="Get Graph Strategy BDD"/>
            <hsc-menu-item @click.native="calculateGraphGameBDD(false)"
                           label="Calculate whole Graph Game BDD"/>
            <hsc-menu-item @click.native="calculateGraphGameBDD(true)"
                           label="Calculate Graph Game BDD incrementally"/>
          </hsc-menu-bar-item>
        </template>
        <hsc-menu-bar-item @click.native="calculateModelCheckingNet" label="Get Model Checking Net"
                           v-if="useModelChecking"/>
        <hsc-menu-bar-item @click.native="checkLtlFormula" label="Check LTL Formula"
                           v-if="useModelChecking"/>
        <hsc-menu-bar-item label="Settings">
          <hsc-menu-item
            :label="showPhysicsControls ? 'Hide physics controls' : 'Show physics controls'"
            @click="showPhysicsControls = !showPhysicsControls"/>
          <hsc-menu-item :label="showPartitions ? 'Hide partitions' : 'Show partitions'"
                         @click="showPartitions = !showPartitions"/>
        </hsc-menu-bar-item>
        <hsc-menu-bar-item @click.native="isLogVisible = !isLogVisible; $refs.menubar.deactivate()"
                           :label="isLogVisible ? 'Hide log' : 'Show log'"/>
        <!--TODO Grey out these buttons or something if these things have already been calculated.-->
        <!--TODO Maybe add a little indicator for each one: "not yet calculated", "in progress", "Finished"-->
        <!--TODO For "calculateExistsWinningStrategy," it could even say whether or not a strategy exists.-->
      </hsc-menu-bar>
      <button @click="showJobList = true"
              style="margin-left: 40px;">View jobs running on server
      </button>
      <v-dialog
        style="display: block;"
        max-width="600"
        v-model="showAboutModal"
        @keydown.esc="showAboutModal = false">
        <template v-slot:activator="{ on }">
          <button style="margin-left: auto; padding-right: 10px; font-size: 18px;"
                  @click="showAboutModal = true">About
          </button>
        </template>
        <v-card>
          <v-card-title
            primary-title
            style="justify-content: space-between;">
            <span>About ADAM Web</span>
            <v-icon standard right
                    @click="showAboutModal = false">
              close
            </v-icon>
          </v-card-title>
          <v-card-text>
            <AboutAdamWeb/>
          </v-card-text>
        </v-card>
      </v-dialog>
      <hsc-menu-context-menu>
        <div style="line-height: 34px; font-size: 18px; padding-right: 10px;">ADAM Web</div>
        <template slot="contextmenu">
          <hsc-menu-item
            :label="useModelChecking ? 'Switch to Distributed Synthesis' : 'Switch to Model Checking'"
            @click="useModelChecking = !useModelChecking; useDistributedSynthesis = !useDistributedSynthesis"/>
        </template>
      </hsc-menu-context-menu>
    </my-theme>

    <div style="display: flex; flex-direction: row; height: calc(100vh - 34.333px); width: 100%;"
         ref="horizontalSplitDiv">
      <div class="flex-column-divider"
           v-on:click="toggleLeftPane"
           v-if="shouldShowRightSide">
        <div :class="isLeftPaneVisible ? 'arrow-left' : 'arrow-right'"></div>
      </div>
      <v-tabs class="tabs-component-full-height" :style="splitLeftSideStyle" id="splitLeftSide"
              v-model="selectedTabLeftSide">
        <v-tab>Petri Game</v-tab>
        <v-tab @click="onSwitchToAptEditor">APT Editor</v-tab>
        <v-tab-item>
          <GraphEditor :graph='petriGame.net'
                       :petriGameId='petriGame.uuid'
                       ref='graphEditorPetriGame'
                       v-on:dragDropEnd='onDragDropEnd'
                       v-on:insertNode='insertNode'
                       v-on:createFlow='createFlow'
                       v-on:createTokenFlow='createTokenFlow'
                       v-on:deleteFlow='deleteFlow'
                       v-on:deleteNode='deleteNode'
                       v-on:renameNode='renameNode'
                       v-on:toggleEnvironmentPlace='toggleEnvironmentPlace'
                       v-on:toggleIsInitialTokenFlow='toggleIsInitialTokenFlow'
                       v-on:setIsSpecial='setIsSpecial'
                       v-on:fireTransition='fireTransition'
                       v-on:setInitialToken='setInitialToken'
                       v-on:setWinningCondition='setWinningCondition'
                       showEditorTools
                       :useModelChecking="useModelChecking"
                       :useDistributedSynthesis="useDistributedSynthesis"
                       :modelCheckingRoutes="modelCheckingRoutes"
                       :shouldShowPhysicsControls="showPhysicsControls"
                       :shouldShowPartitions="showPartitions"
                       :repulsionStrengthDefault="360"
                       :linkStrengthDefault="0.086"/>
        </v-tab-item>
        <v-tab-item>
          <AptEditor :aptFromAdamParser='apt'
                     :aptParseStatus='aptParseStatus'
                     :aptParseError='aptParseError'
                     :aptParseErrorLineNumber='aptParseErrorLineNumber'
                     :aptParseErrorColumnNumber='aptParseErrorColumnNumber'
                     @input='onAptEditorInput'/>
        </v-tab-item>
      </v-tabs>
      <v-tabs class="tabs-component-full-height" :style="splitRightSideStyle" id="splitRightSide">
        <!--TODO Allow closing these tabs-->
        <!--TODO Maybe mark the tabs somehow if the Petri Game has been modified since the tabs were opened-->
        <v-tab v-if="strategyBDD">Strategy BDD</v-tab>
        <v-tab v-if="graphStrategyBDD">Graph Strategy BDD</v-tab>
        <v-tab v-if="graphGameBDD">Graph Game BDD</v-tab>
        <v-tab v-if="modelCheckingNet">Model Checking Net</v-tab>
        <v-tab-item v-if="strategyBDD">
          <GraphEditor :graph='strategyBDD'
                       :petriGameId='petriGame.uuid'
                       ref='graphEditorStrategyBDD'
                       :shouldShowPhysicsControls="showPhysicsControls"/>
        </v-tab-item>
        <v-tab-item v-if="graphStrategyBDD">
          <GraphEditor :graph='graphStrategyBDD'
                       :petriGameId='petriGame.uuid'
                       ref='graphEditorGraphStrategyBDD'
                       :shouldShowPhysicsControls="showPhysicsControls"/>
        </v-tab-item>
        <v-tab-item v-if="graphGameBDD">
          <GraphEditor :graph='graphGameBDD'
                       :petriGameId='petriGame.uuid'
                       ref='graphEditorGraphGameBDD'
                       v-on:toggleStatePostset='toggleGraphGameStatePostset'
                       v-on:toggleStatePreset='toggleGraphGameStatePreset'
                       :shouldShowPhysicsControls="showPhysicsControls"
                       :repulsionStrengthDefault="415"
                       :linkStrengthDefault="0.04"
                       :gravityStrengthDefault="300"/>
        </v-tab-item>
        <v-tab-item v-if="modelCheckingNet">
          <GraphEditor :graph="modelCheckingNet"
                       :petriGameId='petriGame.uuid'
                       :shouldShowPhysicsControls="showPhysicsControls"/>
        </v-tab-item>
      </v-tabs>
    </div>
    <div :style="`color: ${this.notificationColor}`">{{ notificationMessage }}</div>
    <hsc-window-style-metal>
      <hsc-window resizable
                  closeButton
                  :minWidth="200"
                  :minHeight="100"
                  :isOpen.sync="isLogVisible"
                  title="Log"
                  style="z-index: 9999">
        <LogViewer :messages="messageLog"
                   style="height: inherit; width: inherit;"/>
      </hsc-window>
    </hsc-window-style-metal>
  </v-app>
</template>


<script>
  import {aptFileTreeSynthesis, aptFileTreeModelChecking} from './aptExamples'
  import GraphEditor from './components/GraphEditor'
  import AboutAdamWeb from './components/AboutAdamWeb'
  import LogViewer from './components/LogViewer'
  import JobList from './components/JobList'
  import Vue from 'vue'
  import * as axios from 'axios'
  import {debounce} from 'underscore'
  import * as modelCheckingRoutesFactory from './modelCheckingRoutes'

  import Vuetify from 'vuetify'
  import 'vuetify/dist/vuetify.min.css'

  Vue.use(Vuetify)

  import * as VueMenu from '@hscmap/vue-menu'

  Vue.use(VueMenu)
  import MyVueMenuTheme from './menuStyle'

  import * as VueWindow from '@hscmap/vue-window'

  Vue.use(VueWindow)

  import aptExampleLtl from './somewhatSmallExampleLtl.apt'
  import aptExampleDistributedSynthesis from './somewhatSmallExampleNotLtl.apt'
  import HscMenuBarDirectory from './components/hsc-menu-bar-directory'

  import makeWebSocket from '@/logWebSocket'
  import {saveFileAs} from './fileutilities'

  import Split from 'split.js'

  import logging from './logging'
  import AptEditor from './components/AptEditor'

  import {format} from 'date-fns'

  const uuidv4 = require('uuid/v4')

  export default {
    name: 'app',
    props: {
      useModelChecking: {
        type: Boolean,
        required: true
      },
      useDistributedSynthesis: {
        type: Boolean,
        required: true
      }
    },
    components: {
      HscMenuBarDirectory, // TODO decide on import style
      'GraphEditor': GraphEditor,
      'my-theme': MyVueMenuTheme,
      'LogViewer': LogViewer,
      AptEditor,
      JobList,
      AboutAdamWeb
    },
    created: function () {
      console.log(`Adam Web App.vue Configuration:
      useDistributedSynthesis: ${this.useDistributedSynthesis}
      useModelChecking: ${this.useModelChecking}
      baseurl: ${this.baseUrl}`)

      // Save a uuid to identify this browser in the future (e.g. to only show Jobs belonging to this user)
      if (window.localStorage.getItem('browserUuid') === null) {
        window.localStorage.setItem('browserUuid', uuidv4())
      }
      this.browserUuid = window.localStorage.getItem('browserUuid')
      console.log(`browserUuid: ${this.browserUuid}`)

      // Subscribe to logging event bus
      logging.subscribeLog(message => {
        this.messageLog.push(message)
      })
      logging.subscribeErrorNotification(message => {
        this.showNotification(message, '#cc0000')
      })
      logging.subscribeSuccessNotification(message => {
        this.showNotification(message, '#009900')
      })
      logging.subscribeResetNotification(this.resetNotification)
      logging.log('Hello!')

      this.socket = this.initializeWebSocket()

      this.parseAPTToPetriGame(this.apt)
      this.getListOfJobs()
    },
    mounted: function () {
      // Initialize draggable, resizable pane
      this.horizontalSplit = this.createHorizontalSplit()
    },
    data: function () {
      return {
        // Validate whether the given string represents a uuidv4.
        validateBrowserUuid: uuidString => {
          const pattern =
            /^[0-9a-f]{8}-[0-9a-f]{4}-[4-4][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i
          if (pattern.test(uuidString)) {
            return true
          } else {
            return 'The given string does not represent a valid UUID of the form ' +
              '\'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx\', where y is a hexadecimal digit between ' +
              '8 and b.'
          }
        },
        browserUuid: 'browser uuid not yet loaded. (Should have been initialized in mounted hook)',
        browserUuidEntry: '',
        aptFilename: 'apt.txt',
        showSaveAptModal: false,
        showJobList: false,
        showAboutModal: false,
        // True iff the modal dialog with the list of jobs is visible
        availableBDDGraphListings: [], // Listings for all enqueued/finished jobs
        // TODO Rename to just 'availableJobListings' or 'jobListings'
        apt: this.useModelChecking ? aptExampleLtl : aptExampleDistributedSynthesis,
        aptParseStatus: 'success',
        aptParseError: '',
        aptParseErrorLineNumber: -1,
        aptParseErrorColumnNumber: -1,
        // This shows temporary notifications after events happen, e.g. if an error happens when trying to solve a net
        notificationMessage: '',
        notificationColor: '',
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
        // This is a key in a map used on the server to store the Graph Game BDDs.
        // It uniquely identifies our own graphGameBdd on the server.
        graphGameJobKey: {},
        modelCheckingNet: null,
        isLeftPaneVisible: true,
        isLogVisible: false,
        showPhysicsControls: false,
        showPartitions: false,
        messageLog: [],
        snackbarMessage: {
          display: false,
          text: '',
          color: undefined
        },
        horizontalSplit: undefined,  // See "API" section on https://nathancahill.github.io/Split.js/
        horizontalSplitSizes: [50, 50],
        leftPaneMinWidth: 7.65, // Percentage of flexbox container's width
        selectedTabLeftSide: 0
      }
    },
    watch: {
      // When the browser UUID is changed, we should reload the list of jobs and tell the server
      // we want to subscribe to notifications corresponding to our new UUIUD
      browserUuid: function () {
        this.getListOfJobs()
        this.updateWebSocketBrowserUuid()
      },
      // When we open the modal dialog, we should reload the list of jobs
      showJobList: function () {
        if (this.showJobList) {
          this.getListOfJobs()
        }
      },
      apt: function (apt) {
        this.parseAPTToPetriGame(this.apt)
      },
      aptParseStatus: function (status) {
        if (status === 'error') {
          this.switchToAptEditor()
        }
      }
    },
    computed: {
      // This is the prefix used for all HTTP requests to the server.  An empty string means that
      // relative URLs will be used.
      baseUrl: function () {
        return process.env.NODE_ENV === 'development' ? 'http://localhost:4567' : ''
      },
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
        if (this.useDistributedSynthesis) {
          return aptFileTreeSynthesis
        } else {
          return aptFileTreeModelChecking
        }
      },
      splitLeftSideStyle: function () {
        const hideStyle = this.isLeftPaneVisible ? '' : 'display: none;'
        return hideStyle + 'flex-grow: 1;'
      },
      shouldShowRightSide: function () {
        return this.strategyBDD || this.graphStrategyBDD || this.graphGameBDD || this.modelCheckingNet
      },
      splitRightSideStyle: function () {
        return this.shouldShowRightSide ? '' : 'display: none;'
      },
      // Depending on whether we are in development mode or in production, the URLs used for server
      // requests are different.  Production uses relative urls, while dev mode uses hard-coded
      // "localhost:4567/..."
      restEndpoints: function () {
        const endpoints = [
          'calculateExistsWinningStrategy',
          'calculateStrategyBDD',
          'calculateGraphStrategyBDD',
          'calculateGraphGameBDD',
          'getWinningStrategy',
          'getGraphStrategyBDD',
          'getGraphGameBDD',
          'getListOfJobs',
          'cancelJob',
          'deleteJob',
          'toggleGraphGameBDDNodePostset',
          'toggleGraphGameBDDNodePreset',
          'getAptOfPetriGame',
          'updateXYCoordinates',
          'parseApt',
          'insertPlace',
          'createFlow',
          'createTokenFlow',
          'deleteFlow',
          'deleteNode',
          'renameNode',
          'toggleEnvironmentPlace',
          'toggleIsInitialTokenFlow',
          'setIsSpecial',
          'fireTransition',
          'setInitialToken',
          'setWinningCondition',
          'calculateModelCheckingResult',
          'calculateModelCheckingNet'
        ]
        const funs = {}
        endpoints.forEach(endpointName => {
          funs[endpointName] = (options) => {
            return axios.post(this.baseUrl + '/' + endpointName, {
              ...options,
              browserUuid: this.browserUuid
            })
          }
        })
        return funs
      },
      // TODO consider refactoring routes so they are handled more like this and less like the above
      modelCheckingRoutes: function () {
        return modelCheckingRoutesFactory.withPathPrefix(this.baseUrl)
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
            logging.logError(errorMessage)
            throw new Error(errorMessage)
          }
          return `${newUri}//${loc.host}/log`
        } else { // We are running in development mode with a hard-coded URL.  See main-dev.js
          return this.baseUrl.replace('http:', 'ws:').replace('https:', 'ws:') + '/log'
        }
      }
    },
    methods: {
      initializeWebSocket: function () {
        // Connect to the server and subscribe to ADAM's log output
        let socket
        try {
          socket = makeWebSocket(this.webSocketUrl)
        } catch (exception) {
          const errorMessage = 'An exception was thrown when opening the websocket connection to the server.  ' +
            'Server log messages may not be displayed.  Exception: ' + exception.message
          logging.sendErrorNotification(errorMessage)
          throw new Error(errorMessage) // TODO make errors get caught by a global error handler
        }
        socket.$on('message', message => {
          const messageParsed = JSON.parse(message)
          logging.logServerMessage(messageParsed.message, messageParsed.level)
        })
        socket.$on('error', message => {
          logging.sendErrorNotification('The websocket connection to the server threw an error.  ' +
            'ADAM\'s log output might not be displayed.')
        })
        socket.$on('close', () => {
          logging.sendErrorNotification('The websocket connection to the server was closed.  ' +
            'ADAM\'s log output might not be displayed.')
        })
        socket.$on('open', this.updateWebSocketBrowserUuid)
        return socket
      },
      updateWebSocketBrowserUuid: function () {
        // Make sure we get notifications from the server corresponding to our unique session ID
        this.socket.send(JSON.stringify({
          browserUuid: this.browserUuid
        }))
      },
      saveBrowserUuid: function () {
        if (this.browserUuidEntry === this.browserUuid) {
          return
        }
        if (this.validateBrowserUuid(this.browserUuidEntry) === true) {
          const noJobsAreListed = this.availableBDDGraphListings.length === 0
          if (noJobsAreListed || confirm(
            'Changing your browser\'s UUID will cause your current list of jobs to disappear.  ' +
            'The only way to get them back is to re-enter your current UUID (' + this.browserUuid +
            ').  Are you sure?')) {
            this.browserUuid = this.browserUuidEntry
            window.localStorage.setItem('browserUuid', this.browserUuid)
          }
        }
      },
      onClickSaveAptMenuItem: function () {
        // There is a delay here so that the click event doesn't immediately close the
        // modal after it is opened.
        setTimeout(() => this.showSaveAptModal = true, 50)
      },
      createHorizontalSplit: function () {
        const split = Split(['#splitLeftSide', '#splitRightSide'], {
          sizes: this.horizontalSplitSizes,
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
            const leftPaneWidth = split.getSizes()[0]
            const leftPaneShouldCollapse = leftPaneWidth <= this.leftPaneMinWidth
            if (leftPaneShouldCollapse && this.isLeftPaneVisible) {
              logging.logVerbose('collapsing left pane')
              this.isLeftPaneVisible = false
            }
            if (leftPaneShouldCollapse) {
              split.setSizes([0, 100])
            }
            if (!leftPaneShouldCollapse && !this.isLeftPaneVisible) {
              logging.logVerbose('expanding apt editor')
              this.isLeftPaneVisible = true
            }
          },
          onDragEnd: () => {
            if (this.isLeftPaneVisible) {
              // Save current sizes so that we can restore them in toggleLeftPane()
              this.horizontalSplitSizes = split.getSizes()
            }
          }
        })
        return split
      },
      // Hide or show APT editor, restoring its size if appropriate
      toggleLeftPane: function () {
        logging.logVerbose('toggleLeftPane()')
        const restoring = !this.isLeftPaneVisible
        if (restoring) {
          this.horizontalSplit.setSizes(this.horizontalSplitSizes)
        } else {
          this.horizontalSplit.setSizes([0, 100])
        }
        this.isLeftPaneVisible = !this.isLeftPaneVisible
      },
      // Load APT from a text file stored on the user's local filesystem
      // See https://developer.mozilla.org/en-US/docs/Web/API/File/Using_files_from_web_applications
      onFileSelected: function (changeEvent) {
        logging.logVerbose('The user selected a file in the file selector')
        const file = changeEvent.target.files[0]
        logging.logObject(file)
        const reader = new FileReader()
        reader.onloadend = () => {
          // TODO verify that the file is reasonable (i.e. plain text, not a binary or other weird file)
          logging.logVerbose('The file selected by the user is finished loading.  Updating text editor contents')
          this.onAptExampleSelected(reader.result)
        }
        reader.readAsText(file)
      },
      loadAptFromFile: function () {
        document.getElementById('file-picker').click()
      },
      saveAptToFile: function () {
        this.savePetriGameAsAPT().then(() => saveFileAs(this.apt, this.aptFilename))
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
      switchToAptEditor: function () {
        this.selectedTabLeftSide = 1
      },
      switchToStrategyBDDTab: function () {
        console.log('TODO implement switchToStrategyBDDTab')
        // TODO: This should be easy, but it's kind of inconvenient because they can only be
        // identified by number, and the number of tabs is variable.
        // you do it with e.g. this.selectedTabRightSide = 0 to switch to the 0th tab
      },
      switchToGraphStrategyBDDTab: function () {
        console.log('TODO implement switchToGraphStrategyBDDTab')
      },
      switchToGraphGameBDDTab: function () {
        console.log('TODO implement switchToGraphGameBDDTab')
      },
      // Send APT to backend and parse it, then display the resulting Petri Game.
      // This is debounced using Underscore: http://underscorejs.org/#debounce
      parseAPTToPetriGame: debounce(function (apt) {
        logging.logVerbose('Sending APT source code to backend.')
        this.restEndpoints.parseApt({
          params: {
            apt: apt
          }
        }).then(response => {
          switch (response.data.status) {
            case 'success':
              logging.logVerbose('Successfully parsed APT. Received Petri Game from backend.')
              logging.logObject(response)
              this.petriGame = response.data.graph
              this.aptParseStatus = 'success'
              this.aptParseError = ''
              this.aptParseErrorLineNumber = -1
              this.aptParseErrorColumnNumber = -1
              break
            case 'error':
              this.aptParseStatus = 'error'
              this.aptParseError = response.data.message
              this.aptParseErrorLineNumber = response.data.lineNumber
              this.aptParseErrorColumnNumber = response.data.columnNumber
              logging.logVerbose(`There was an error when we tried to parse the APT: ${response.data.message}`)
              break
            default:
              logging.logError('We got an unexpected response from the server when trying to parse the APT:')
              logging.logError(response)
              this.aptParseStatus = 'error'
              this.aptParseError = 'Unexpected response from server: ' + JSON.stringify(response, null, 2)
              this.aptParseErrorLineNumber = -1
              this.aptParseErrorColumnNumber = -1
              break
          }
        }).catch(() => {
          logging.logError('Network error when trying to parse APT')
        })
        this.aptParseStatus = 'running'
      }, 200),
      calculateModelCheckingNet: function () {
        this.$refs.menubar.deactivate()
        this.restEndpoints.calculateModelCheckingNet({
          params: {
            formula: this.$refs.graphEditorPetriGame.ltlFormula
          },
          petriGameId: this.petriGame.uuid
        }).then(response => {
          this.withErrorHandling(response, response => {
            // Show the result if it is finished within 5-10 seconds.  Otherwise just show a message
            if (response.data.jobComplete) {
              this.modelCheckingNet = net
            } else {
              // The message from server will explain that the job has been enqueued.
              // TODO Provide a notification after it is finished
              logging.sendSuccessNotification(response.data.message)
            }
          })
        }).catch(() => {
          logging.logError('Network error in calculateModelCheckingNet')
        })
      },
      checkLtlFormula: function () {
        // this.$refs.menubar.deactivate()
        this.restEndpoints.calculateModelCheckingResult({
          params: {
            formula: this.$refs.graphEditorPetriGame.ltlFormula
          },
          petriGameId: this.petriGame.uuid
        }).then(response => {
          this.withErrorHandling(response, response => {
            // Show the result if it is finished within 5-10 seconds.  Otherwise just show a message
            if (response.data.jobComplete) {
              this.apt = response.data.jobKey.canonicalApt
              switch (response.data.result) {
                case 'TRUE':
                  logging.sendSuccessNotification(
                    'The result of model checking is: ' + response.data.result)
                  break
                case 'UNKNOWN':
                case 'FALSE':
                default:
                  logging.sendErrorNotification(
                    'The result of model checking is: ' + response.data.result)
                  break
              }
            } else {
              // The message from server will explain that the job has been enqueued.
              // TODO Provide a notification after it is finished
              logging.sendSuccessNotification(response.data.message)
            }
          })
        }).catch(() => {
          logging.logError('Network error in calculateModelCheckingResult')
        })
      },
      calculateExistsWinningStrategy: function () {
        logging.sendSuccessNotification('Sent a request to the server to see if there is a winning strategy')
        this.restEndpoints.calculateExistsWinningStrategy({
          petriGameId: this.petriGame.uuid,
          params: {}
        }).then(response => {
          this.withErrorHandling(response, response => {
            // Show the result if it is finished within 5-10 seconds.  Otherwise just show a message
            if (response.data.jobComplete) {
              this.apt = response.data.jobKey.canonicalApt
              this.petriGame.hasWinningStrategy = response.data.result
              if (this.petriGame.hasWinningStrategy) {
                // TODO consider displaying the info in a more persistent way, e.g. by colorizing the button "exists winning strategy".
                logging.sendSuccessNotification('Yes, there is a winning strategy for this Petri Game.')
                // We expect an updated petriGame here because there might have been partition annotations added.
                // this.petriGame.net = response.data.petriGame
              } else {
                logging.sendErrorNotification('No, there is no winning strategy for this Petri Game.')
              }
            } else {
              // The message from server will explain that the job has been enqueued.
              // TODO Provide a notification after it is finished
              logging.sendSuccessNotification(response.data.message)
            }
          })
        }).catch(() => {
          logging.logError('Network error in calculateExistsWinningStrategy')
        })
      },
      calculateStrategyBDD: function () {
        this.$refs.menubar.deactivate()
        const uuid = this.petriGame.uuid
        logging.sendSuccessNotification('Sent request to server to calculate the winning strategy')
        this.restEndpoints.calculateStrategyBDD({
          petriGameId: uuid,
          params: {}
        }).then(response => {
          this.withErrorHandling(response, response => {
            // Load the strategy BDD if it is finished within 5-10 seconds.  Otherwise just show a message
            if (response.data.jobComplete) {
              this.strategyBDD = response.data.result
              this.strategyBDD.uuid = uuid
              // We expect an updated petriGame here because there might have been partition annotations added.
              this.petriGame.net = response.data.petriGame
              this.switchToStrategyBDDTab()
              this.apt = response.data.jobKey.canonicalApt
              logging.sendSuccessNotification(response.data.message)
            } else {
              // The message from server will explain that the job has been enqueued.
              // TODO Provide a notification after it is finished
              logging.sendSuccessNotification(response.data.message)
            }
          })
        }).catch(() => {
          logging.logError('Network error in calculateStrategyBDD')
        })
      },
      calculateGraphStrategyBDD: function () {
        const uuid = this.petriGame.uuid
        logging.sendSuccessNotification('Sent request to server to calculate the Graph Strategy BDD')
        this.restEndpoints.calculateGraphStrategyBDD({
          petriGameId: uuid,
          params: {}
        }).then(response => {
          this.withErrorHandling(response, response => {
            if (response.data.jobComplete) {
              this.graphStrategyBDD = response.data.result
              this.graphStrategyBDD.uuid = uuid
              // We expect an updated petriGame here because there might have been partition annotations added.
              this.petriGame.net = response.data.petriGame
              this.switchToGraphStrategyBDDTab()
              this.apt = response.data.jobKey.canonicalApt
              logging.sendSuccessNotification(response.data.message)
            } else {
              logging.sendSuccessNotification(response.data.message)
            }
          })
        }).catch(() => {
          logging.logError('Network error in calculateGraphStrategyBDD')
        })
      },
      getListOfJobs: function () {
        this.restEndpoints.getListOfJobs()
          .then(response => {
            this.availableBDDGraphListings = response.data.listings
          })
      },
      // Load the Graph Game BDD corresponding to the given job key
      getGraphGameBdd: function (jobKey) {
        this.restEndpoints.getGraphGameBDD({
          jobKey
        }).then(response => {
          switch (response.data.status) {
            case 'error':
              logging.sendErrorNotification(response.data.message)
              break
            case 'success':
              this.apt = jobKey.canonicalApt
              this.graphGameBDD = response.data.result
              this.graphGameJobKey = jobKey
              this.switchToGraphGameBDDTab()
              logging.sendSuccessNotification('Loaded Graph Game BDD')
          }
        })
      },
      getWinningStrategy: function (jobKey) {
        this.restEndpoints.getWinningStrategy({
          jobKey: jobKey
        }).then(response => {
          switch (response.data.status) {
            case 'error':
              logging.sendErrorNotification(response.data.message)
              break
            case 'success':
              this.apt = jobKey.canonicalApt
              this.strategyBDD = response.data.result
              this.switchToStrategyBDDTab()
              logging.sendSuccessNotification('Loaded Winning Strategy')
          }
        })
      },
      getGraphStrategyBdd: function (jobKey) {
        this.restEndpoints.getGraphStrategyBDD({
          jobKey
        }).then(response => {
          switch (response.data.status) {
            case 'error':
              logging.sendErrorNotification(response.data.message)
              break
            case 'success':
              this.apt = jobKey.canonicalApt
              this.graphStrategyBDD = response.data.result
              this.switchToGraphStrategyBDDTab()
              logging.sendSuccessNotification('Loaded Graph Strategy BDD')
          }
        })
      },
      cancelJob: function ({jobKey, type}) {
        logging.sendSuccessNotification('Sent request to cancel the job of ' + type)
        this.restEndpoints.cancelJob({
          jobKey,
          type
        }).then(response => {
          switch (response.data.status) {
            case 'error':
              logging.sendErrorNotification(response.data.message)
              break
            case 'success':
              logging.sendSuccessNotification('Cancelled the job.  Note: It might take a ' +
                'little while for the job to actually stop.  This is a limitation of the ' +
                'libraries used by ADAM.')
          }
        }).then(this.getListOfJobs)
      },
      deleteJob: function ({jobKey, type}) {
        logging.sendSuccessNotification('Sent request to delete the job')
        this.restEndpoints.deleteJob({
          jobKey,
          type
        }).then(response => {
          switch (response.data.status) {
            case 'error':
              logging.sendErrorNotification(response.data.message)
              break
            case 'success':
              logging.sendSuccessNotification('Cancelled the job and deleted it from the ' +
                'list of jobs.  Note: It might take a little while for the job to actually' +
                ' stop if it was running.  This is a limitation of the libraries used by' +
                ' ADAM.')
          }
        }).then(this.getListOfJobs)
      },
      calculateGraphGameBDD: function (incremental) {
        const uuid = this.petriGame.uuid
        logging.sendSuccessNotification('Sent request to server to calculate the Graph Game BDD')
        this.restEndpoints.calculateGraphGameBDD({
          petriGameId: uuid,
          params: {
            incremental
          }
        }).then(response => {
          this.withErrorHandling(response, response => {
            // Load the graph game BDD if it is finished within 5-10 seconds.  Otherwise just show a message
            if (response.data.jobComplete) {
              this.apt = response.data.jobKey.canonicalApt
              this.graphGameBDD = response.data.result
              this.graphGameJobKey = response.data.jobKey
              // TODO Get Petri Game from server in caes partition annotations have been added
              // this.petriGame.net = response.data.petriGame
              this.switchToGraphGameBDDTab()
              logging.sendSuccessNotification(response.data.message)
            } else {
              // The message from server will explain that the job has been enqueued.
              // TODO Provide a notification after it is finished
              logging.sendSuccessNotification(response.data.message)
            }
          })
        }).catch(() => {
          logging.logError('Network error in calculateGraphGameBDD')
        })
      },
      toggleGraphGameStatePostset: function (stateId) {
        this.restEndpoints.toggleGraphGameBDDNodePostset({
          jobKey: this.graphGameJobKey,
          stateId: stateId
        }).then(response => {
          this.withErrorHandling(response, response => {
            this.graphGameBDD = response.data.graphGameBDD
          })
        }).catch(() => {
          logging.logError('Network error')
        })
      },
      toggleGraphGameStatePreset: function (stateId) {
        this.restEndpoints.toggleGraphGameBDDNodePreset({
          jobKey: this.graphGameJobKey,
          stateId: stateId
        }).then(response => {
          this.withErrorHandling(response, response => {
            this.graphGameBDD = response.data.graphGameBDD
          })
        }).catch(() => {
          logging.logError('Network error')
        })
      },
      onAptEditorInput: function (apt) {
        this.apt = apt
      },
      onSwitchToAptEditor: function () {
        const isAptEditorAlreadySelected = this.selectedTabLeftSide === 1
        if (isAptEditorAlreadySelected) {
          return
        }
        logging.logVerbose('Switching to APT editor')
        this.savePetriGameAsAPT()
      },
      saveXYCoordinatesOnServer: function () {
        // Our graph editor should give us an object with Node IDs as keys and x,y coordinates as values.
        // We send those x,y coordinates to the server, and the server saves them as annotations
        // into the PetriGame object.
        const nodePositions = this.$refs.graphEditorPetriGame.getNodeXYCoordinates()
        return this.restEndpoints.updateXYCoordinates({
          petriGameId: this.petriGame.uuid,
          nodeXYCoordinateAnnotations: nodePositions
        }).then(response => {
          this.withErrorHandling(response, response => {
            // TODO assert that the x/y coordinates we recieve match those we sent?
          })
        }, reason => {
          // This function gets called if the promise is rejected (i.e. the http request failed)
          logging.logError('saveXYCoordinatesOnServer(): An error occurred. ' + reason)
          throw new Error(reason)
        })
      },
      // Return a promise with the return value of the new apt
      // TODO refactor 'withErrorHandling'. It's annoying to have to type 'return' twice.
      getAptOfPetriGame: function () {
        return this.restEndpoints.getAptOfPetriGame({
          petriGameId: this.petriGame.uuid
        }).then(response => {
          return this.withErrorHandling(response, response => {
            return response.data.apt
          })
        })
      },
      // Save xy coordinates on the server and then get the new updated APT back
      savePetriGameAsAPT: function () {
        this.saveXYCoordinatesOnServer()
          .then(this.getAptOfPetriGame)
          .then(apt => {
            this.apt = apt
          })
      },
      createFlow: function (flowSpec) {
        console.log('processing createFlow event')
        this.restEndpoints.createFlow({
          petriGameId: this.petriGame.uuid,
          source: flowSpec.source,
          destination: flowSpec.destination
        }).then(response => {
          this.withErrorHandling(response, response => {
            this.petriGame.net = response.data.result
          })
        }).catch(() => {
          logging.logError('Network error')
        })
      },
      createTokenFlow: function ({source, transition, postset}) {
        console.log('processing createTokenFlow event')
        this.restEndpoints.createTokenFlow({
          petriGameId: this.petriGame.uuid,
          source,
          transition,
          postset
        }).then(response => {
          this.withErrorHandling(response, response => {
            this.petriGame.net = response.data.result
          })
        }).catch(() => {
          logging.logError('Network error')
        })
      },
      deleteFlow: function ({sourceId, targetId}) {
        console.log('processing deleteFlow event')
        this.restEndpoints.deleteFlow({
          petriGameId: this.petriGame.uuid,
          sourceId,
          targetId
        }).then(response => {
          this.withErrorHandling(response, response => {
            this.petriGame.net = response.data.result
          })
        }).catch(() => {
          logging.logError('Network error')
        })
      },
      deleteNode: function (nodeId) {
        console.log('processing deleteNode event for node id ' + nodeId)
        this.restEndpoints.deleteNode({
          petriGameId: this.petriGame.uuid,
          nodeId: nodeId
        }).then(response => {
          this.withErrorHandling(response, response => {
            this.petriGame.net = response.data.result
          })
        }).catch(() => {
          logging.logError('Network error')
        })
      },
      renameNode: function ({xOld, yOld, idOld, idNew}) {
        console.log('processing renameNode event')
        console.log(`renaming node '${idOld}' to '${idNew}'`)
        this.restEndpoints.renameNode({
          petriGameId: this.petriGame.uuid,
          nodeIdOld: idOld,
          nodeIdNew: idNew
        }).then(response => {
          this.withErrorHandling(response, response => {
            // Preserve the x/y coordinates of the renamed node
            const newNet = response.data.result
            newNet.nodePositions = {}
            newNet.nodePositions[idNew] = {
              x: xOld,
              y: yOld
            }
            this.petriGame.net = newNet
          })
        }).catch(() => {
          logging.logError('Network error')
        })
      },
      toggleEnvironmentPlace: function (nodeId) {
        console.log('processing toggleEnvironmentPlace event')
        this.restEndpoints.toggleEnvironmentPlace({
          petriGameId: this.petriGame.uuid,
          nodeId: nodeId
        }).then(response => {
          this.withErrorHandling(response, response => {
            this.petriGame.net = response.data.result
          })
        }).catch(() => {
          logging.logError('Network error')
        })
      },
      toggleIsInitialTokenFlow: function (nodeId) {
        console.log('processing toggleIsInitialTokenFlow event')
        this.restEndpoints.toggleIsInitialTokenFlow({
          petriGameId: this.petriGame.uuid,
          nodeId: nodeId
        }).then(response => {
          this.withErrorHandling(response, response => {
            this.petriGame.net = response.data.result
          })
        }).catch(() => {
          logging.logError('Network error')
        })
      },
      setIsSpecial: function ({nodeId, newSpecialValue}) {
        console.log('processing setIsSpecial event')
        this.restEndpoints.setIsSpecial({
          petriGameId: this.petriGame.uuid,
          nodeId,
          newSpecialValue
        }).then(response => {
          this.withErrorHandling(response, response => {
            this.petriGame.net = response.data.result
          })
        }).catch(() => {
          logging.logError('Network error')
        })
      },
      fireTransition: function (transitionId) {
        console.log('firing transition with ID ' + transitionId)
        this.restEndpoints.fireTransition({
          petriGameId: this.petriGame.uuid,
          transitionId: transitionId
        }).then(response => {
          this.withErrorHandling(response, response => {
            logging.sendSuccessNotification('Fired transition ' + transitionId)
            this.petriGame.net = response.data.result
          })
        }).catch(() => {
          logging.logError('Network error')
        })
      },
      setInitialToken: function ({nodeId, tokens}) {
        console.log('processing setInitialToken event')
        this.restEndpoints.setInitialToken({
          petriGameId: this.petriGame.uuid,
          nodeId: nodeId,
          tokens: tokens
        }).then(response => {
          this.withErrorHandling(response, response => {
            this.petriGame.net = response.data.result
          })
        }).catch(() => {
          logging.logError('Network error')
        })
      },
      setWinningCondition: function (winningCondition) {
        this.restEndpoints.setWinningCondition({
          petriGameId: this.petriGame.uuid,
          winningCondition: winningCondition
        }).then(response => {
          this.withErrorHandling(response, response => {
            this.petriGame.net = response.data.result
          })
        }).catch(() => {
          logging.logError('Network error')
        })
      },
      insertNode: function (nodeSpec) {
        console.log('processing insertNode event')
        this.restEndpoints.insertPlace({
          petriGameId: this.petriGame.uuid,
          nodeType: nodeSpec.type,
          x: nodeSpec.x,
          y: nodeSpec.y
        }).then(response => {
          this.withErrorHandling(response, response => {
            this.petriGame.net = response.data.result
          })
        }).catch(() => {
          logging.logError('Network error')
        })
      },
      onDragDropEnd: function (graph) {
        logging.logVerbose('App: Received dragDrop event from graph editor. Saving x/y coordinates.')
        this.saveXYCoordinatesOnServer()
      },
      onAptExampleSelected: function (apt) {
        // Let the Graph Editor know that a new petri game has been loaded.
        // This needs to be handled differently than an incremental edit to an already loaded
        // Petri Game, because when we load a new APT file, we want all of the nodes' positions
        // to be reset.
        this.$refs.graphEditorPetriGame.onLoadNewPetriGame()
        this.apt = apt
        this.isLeftPaneVisible = true
      },
      // TODO Throw an exception here so that the promises this function is used in do not get
      // mistakenly fulfilled.
      withErrorHandling: function (response, onSuccessCallback) {
        switch (response.data.status) {
          case 'success':
            return onSuccessCallback(response)
            break
          case 'error':
            logging.sendErrorNotification(response.data.message)
            break
          default:
            logging.sendErrorNotification(`Received a malformed response from the server: ${response.data}`)
        }
      },
      showNotification: function (message, color) {
        const timeStamp = format(new Date(), 'HH:mm:ss')
        this.notificationMessage = timeStamp + ' ' + message
        this.notificationColor = color
      },
      resetNotification: function () {
        this.notificationMessage = ''
      }
    }
  }
</script>

<style>
  @font-face {
    font-family: 'Material Icons';
    font-style: normal;
    font-weight: 400;
    src: local('Material Icons'), local('MaterialIcons-Regular'),
    url('./assets/fonts/MaterialIcons-Regular.woff2') format('woff2'),
    url('./assets/fonts/MaterialIcons-Regular.woff') format('woff');
  }

  .material-icons {
    font-family: 'Material Icons';
    font-weight: normal;
    font-style: normal;
    font-size: 24px; /* Preferred icon size */
    display: inline-block;
    line-height: 1;
    text-transform: none;
    letter-spacing: normal;
    word-wrap: normal;
    white-space: nowrap;
    direction: ltr;

    /* Support for all WebKit browsers. */
    -webkit-font-smoothing: antialiased;
    /* Support for Safari and Chrome. */
    text-rendering: optimizeLegibility;

    /* Support for Firefox. */
    -moz-osx-font-smoothing: grayscale;

    /* Support for IE. */
    font-feature-settings: 'liga';
  }

  /* roboto-regular - latin */
  @font-face {
    font-family: 'Roboto';
    font-style: normal;
    font-weight: 400;
    src: local('Roboto'), local('Roboto-Regular'),
    url('./assets/fonts/roboto-v18-latin-regular.woff2') format('woff2'),
      /* Chrome 26+, Opera 23+, Firefox 39+ */ url('./assets/fonts/roboto-v18-latin-regular.woff') format('woff'); /* Chrome 6+, Firefox 3.6+, IE 9+, Safari 5.1+ */
  }

  #app {
    font-family: 'Roboto', Helvetica, Arial, sans-serif;
    -webkit-font-smoothing: antialiased;
    -moz-osx-font-smoothing: grayscale;
    color: #2c3e50;
    height: 100vh;
  }

  .tabs-component-full-height,
  .tabs-component-full-height > .v-window > .v-window__container,
  .tabs-component-full-height > .v-window > .v-window__container > .v-window-item {
    height: 100%;
  }

  .tabs-component-full-height {
    display: flex;
    flex-direction: column;
  }

  .tabs-component-full-height > .v-tabs__bar {
    flex-grow: 0;
    flex-shrink: 0;
    flex-basis: auto;
  }

  .tabs-component-full-height > .v-window {
    flex-grow: 1;
    flex-shrink: 1;
    flex-basis: available;
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

  .v-card__title {
    font-size: 18px;
  }
</style>
