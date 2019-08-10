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
            :jobListings="jobListings"
            :useModelChecking="useModelChecking"
            style="background-color: white;"
            @loadJob="openOrAddTab"
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
    <MyVueMenuTheme
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
    </MyVueMenuTheme>

    <div style="display: flex; flex-direction: row; height: calc(100vh - 34.333px); width: 100%;"
         ref="horizontalSplitDiv">
      <div class="flex-column-divider"
           v-on:click="toggleLeftPane"
           v-if="shouldShowRightSide">
        <div :class="isLeftPaneVisible ? 'arrow-left' : 'arrow-right'"></div>
      </div>
      <v-tabs class="tabs-component-full-height" :style="splitLeftSideStyle" id="splitLeftSide"
              v-model="selectedTabLeftSide">
        <draggable v-model="tabsLeftSide" class="v-tabs__container"
                   @start="tabDragStart"
                   @end="(evt) => tabDragEnd(evt, 'left')"
                   @choose="onTabChosen">
          <!--We include the tab's index in the key so that this component will re-render when
          the tabs' order changes.  That's necessary so that the 'current tab' indicator will
          update appropriately after a drag-drop.-->
          <v-tab v-for="(tab, index) in tabsLeftSide"
                 :key="`${index}-${tab.uuid}`"
                 @click="onSwitchToTabLeftSide(tab)"
                 :href="`#tab-${tab.uuid}`">
            {{ tab.name }}
            <v-icon standard right
                    v-if="tab.isCloseable"
                    @click="closeTab(tab, 'left')">
              close
            </v-icon>
          </v-tab>
        </draggable>
        <v-tab-item v-for="tab in tabsLeftSide"
                    :key="tab.uuid"
                    :value="`tab-${tab.uuid}`"
                    :transition="false"
                    :reverse-transition="false">
          <keep-alive>
            <div style="position: relative; height: 100%; width: 100%;"
                 v-if="tab.type === 'petriGameEditor'">
              <!--<ToolPicker style="position: absolute; z-index: 5; width: 5%; display: none;"/>-->
              <GraphEditor :graph='petriGame.net'
                           :lastTransitionFired='lastPetriGameTransitionFired'
                           :petriNetId='petriGame.uuid'
                           ref='graphEditorPetriGame'
                           v-on:dragDropEnd='onDragDropEnd'
                           v-on:insertNode='insertNode'
                           v-on:createFlow='createFlow'
                           v-on:createTransit='createTransit'
                           v-on:deleteFlow='deleteFlow'
                           v-on:deleteNode='deleteNode'
                           v-on:renameNode='renameNode'
                           v-on:toggleEnvironmentPlace='toggleEnvironmentPlace'
                           v-on:toggleIsInitialTransit='toggleIsInitialTransit'
                           v-on:setIsSpecial='setIsSpecial'
                           v-on:fireTransition='fireTransition'
                           v-on:setInitialToken='setInitialToken'
                           v-on:setWinningCondition='setWinningCondition'
                           v-on:setFairness='setFairness'
                           v-on:setInhibitorArc='setInhibitorArc'
                           showEditorTools
                           :useModelChecking="useModelChecking"
                           :useDistributedSynthesis="useDistributedSynthesis"
                           :modelCheckingRoutes="modelCheckingRoutes"
                           :shouldShowPhysicsControls="showPhysicsControls"
                           :shouldShowPartitions="showPartitions"
                           :repulsionStrengthDefault="360"
                           :linkStrengthDefault="0.086"/>
            </div>
            <AptEditor v-else-if="tab.type === 'aptEditor'"
                       :aptFromAdamParser='apt'
                       :aptParseStatus='aptParseStatus'
                       :aptParseError='aptParseError'
                       :aptParseErrorLineNumber='aptParseErrorLineNumber'
                       :aptParseErrorColumnNumber='aptParseErrorColumnNumber'
                       @input='onAptEditorInput'/>
            <div v-else>
              Tab type not yet implemented: {{ tab.type }}
            </div>
          </keep-alive>
        </v-tab-item>
      </v-tabs>
      <v-tabs class="tabs-component-full-height" :style="splitRightSideStyle" id="splitRightSide"
              v-model="selectedTabRightSide">
        <draggable v-model="visibleJobsRightSide" class="v-tabs__container"
                   @start="tabDragStart"
                   @end="(evt) => tabDragEnd(evt, 'right')"
                   @choose="onTabChosen">
          <!--We include the tab's index in the key so that this component will re-render when
          the tabs' order changes.  That's necessary so that the 'current tab' indicator will
          update appropriately after a drag-drop.-->
          <!--TODO Mark the tabs somehow if the Petri Game has been modified since the tabs were
          opened-->
          <v-tab v-for="(tab, index) in tabsRightSide"
                 :key="`${index}-${tab.uuid}`"
                 :href="`#tab-${tab.uuid}`">
            {{ formatTabTitle(tab) }}
            <!--Show a spinny circle to indicate job is in the process of being canceled.
            The tab will be closed after the job is fully canceled.
            (see websocket message handler 'jobStatusChanged')-->
            <v-progress-circular
              v-if="tab.jobStatus === 'CANCELING'"
              indeterminate
            />
            <!--Show an X to close the tab/cancel the running job-->
            <v-icon
              v-else-if="tab.isCloseable"
              standard right
              @click="closeTab(tab, 'right')">
              close
            </v-icon>
            <template
              v-else/>
          </v-tab>
        </draggable>
        <v-tab-item v-for="tab in tabsRightSide"
                    :key="tab.uuid"
                    :value="`tab-${tab.uuid}`"
                    :transition="false"
                    :reverse-transition="false">
          <keep-alive>
            <div v-if="tab.type === 'errorMessage'">
              Error: {{ tab.message }}
            </div>
            <div v-else-if="tab.jobStatus !== 'COMPLETED'">
              <div v-if="tab.jobStatus === 'FAILED'">
                Job failed.
              </div>
              <div v-else>
                Job not completed.
              </div>

              <div>Job type: {{ tab.type }}</div>
              <div>Job status: {{ tab.jobStatus }}</div>
              <div v-if="tab.jobStatus !== 'FAILED'">Queue position: {{ tab.queuePosition }}</div>
              <div v-if="tab.jobStatus === 'FAILED'">Failure reason: {{ tab.failureReason }}</div>
            </div>
            <div v-else-if="tab.type === 'EXISTS_WINNING_STRATEGY'">
              <template v-if="tab.result === true">
                Yes, there is a winning strategy for this net.
              </template>
              <template v-else-if="tab.result === false">
                No, there is no winning strategy for this net.
              </template>
              <template v-else>
                Error: Invalid value for job result: {{ tab.result }}
                <div>
                  (This shouldn't happen. Please file a bug. :)
                </div>
              </template>
            </div>
            <GraphEditor v-else-if="tab.type === 'WINNING_STRATEGY'"
                         :graph="tab.result"
                         :shouldShowPhysicsControls="showPhysicsControls"/>
            <GraphEditor v-else-if="tab.type === 'GRAPH_STRATEGY_BDD'"
                         :graph="tab.result"
                         :shouldShowPhysicsControls="showPhysicsControls"/>
            <GraphEditor v-else-if="tab.type === 'GRAPH_GAME_BDD'"
                         :graph='tab.result'
                         v-on:toggleStatePostset="stateId => toggleGraphGameStatePostset(stateId, tab.jobKey)"
                         v-on:toggleStatePreset="stateId => toggleGraphGameStatePreset(stateId, tab.jobKey)"
                         :shouldShowPhysicsControls="showPhysicsControls"
                         :repulsionStrengthDefault="415"
                         :linkStrengthDefault="0.04"
                         :gravityStrengthDefault="300"/>
            <GraphEditor v-else-if="tab.type === 'MODEL_CHECKING_NET'"
                         :graph="tab.result"
                         :shouldShowPhysicsControls="showPhysicsControls"/>
            <div v-else-if="tab.type === 'MODEL_CHECKING_RESULT'">
              <h2>Model checking result</h2>
              <div>Formula: {{ tab.jobKey.requestParams.formula }}</div>
              <div>Result: {{ tab.result }}</div>
            </div>
            <div v-else>
              <div>Tab type not yet implemented: {{ tab.type }}</div>
              <div>
                Tab contents:
                <div style="white-space: pre-wrap;">{{ tab }}</div>
              </div>
            </div>
          </keep-alive>
        </v-tab-item>
      </v-tabs>
    </div>
    <div :style="`color: ${this.notificationColor}`">
      {{ notificationMessage }}
      <a @click="quickAction.action">
        {{ quickAction.actionName }}
      </a>
    </div>
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
  import ToolPicker from './components/ToolPicker'
  import Vue from 'vue'
  import * as axios from 'axios'
  import {debounce, isEqual} from 'underscore'
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

  import makeWebSocket from './logWebSocket'
  import {saveFileAs} from './fileutilities'

  import Split from 'split.js'

  import logging from './logging'
  import AptEditor from './components/AptEditor'

  import {format} from 'date-fns'

  import draggable from 'vuedraggable'
  import {formatJobType} from './jobType'

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
      draggable,
      HscMenuBarDirectory,
      GraphEditor,
      MyVueMenuTheme,
      LogViewer,
      AptEditor,
      JobList,
      ToolPicker,
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
      logging.subscribeErrorNotification(({message, actionName, action}) => {
        this.showNotification(message, '#cc0000')
        if (actionName !== undefined && action !== undefined) {
          this.showQuickActionLink(actionName, action)
        }
      })
      logging.subscribeSuccessNotification(message => {
        this.showNotification(message, '#009900')
      })
      logging.subscribeResetNotification(this.resetNotification)
      logging.log('Hello!')

      this.socket = this.initializeWebSocket(0)

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
        jobListings: [], // Listings for all enqueued/finished jobs
        // TODO Rename to just 'availableJobListings' or 'jobListings'
        apt: this.useModelChecking ? aptExampleLtl : aptExampleDistributedSynthesis,
        aptParseStatus: 'success',
        aptParseError: '',
        aptParseErrorLineNumber: -1,
        aptParseErrorColumnNumber: -1,
        // This shows temporary notifications after events happen, e.g. if an error happens when trying to solve a net
        notificationMessage: '',
        notificationColor: '',
        // This creates a link that the user can click in the notification bar to do a certain action.
        quickAction: {
          actionName: '',
          action: () => {
          }
        },
        tabsLeftSide: [
          {
            type: 'petriGameEditor',
            name: 'Petri Game',
            uuid: 'PetriGameTab', // this tab is hard-coded
            isCloseable: false
          },
          {
            type: 'aptEditor',
            name: 'APT Editor',
            uuid: 'AptEditorTab',
            isCloseable: false
          }
        ],
        visibleJobsRightSide: [],
        petriGame: {
          net: {
            links: [],
            nodes: []
          },
          uuid: 'abcfakeuuid123'
        },
        lastPetriGameTransitionFired: {
          id: '___AAAAA fake transition ID',
          successful: false,
          timestamp: new Date(0)
        },
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
        // Name of the currently selected tab.  'tab-<uuid>'
        selectedTabLeftSide: 'tab-PetriGameTab',
        selectedTabRightSide: 'tab-PetriGameTab2' // TODO change.  This is only for testing
      }
    },
    watch: {
      tabsRightSide: function () {
        console.log('Watcher tabsRightSide:')
        console.log(this.tabsRightSide)
      },
      // When the browser UUID is changed, we should reload the list of jobs and tell the server
      // we want to subscribe to notifications corresponding to our new UUIUD
      browserUuid: function () {
        this.getListOfJobs()
        if (this.socket.isReady()) {
          this.updateWebSocketBrowserUuid()
        }
      },
      // When we open the modal dialog, we should reload the list of jobs
      showJobList: function () {
        if (this.showJobList) {
          this.getListOfJobs()
        }
      },
      apt: function (apt) {
        this.parseAPTToPetriGame(apt)
      },
      aptParseStatus: function (status) {
        if (status === 'error') {
          this.switchToAptEditor()
        }
      }
    },
    computed: {
      tabsRightSide: function () {
        return this.visibleJobsRightSide.map((jobKey) => jobKeyToTab(this.jobListings, jobKey))

        function jobKeyToTab (jobListings, jobKey) {
          const matchingJobListing = jobListings.find(listing => isEqual(listing.jobKey, jobKey))
          if (matchingJobListing) {
            return {
              ...matchingJobListing,
              name: matchingJobListing.type,
              uuid: JSON.stringify(jobKey),
              isCloseable: true
            }
          } else {
            return {
              name: 'Invalid job',
              type: 'errorMessage',
              uuid: JSON.stringify(jobKey),
              isCloseable: true,
              message: 'The jobKey associated with this tab was not found in the list of jobs',
              jobKey
            }
          }
        }
      },
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
        return this.tabsRightSide.length !== 0
      },
      splitRightSideStyle: function () {
        return this.shouldShowRightSide ? '' : 'display: none;'
      },
      // Depending on whether we are in development mode or in production, the URLs used for server
      // requests are different.  Production uses relative urls, while dev mode uses hard-coded
      // "localhost:4567/..."
      restEndpoints: function () {
        const endpoints = [
          'queueJob',
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
          'createTransit',
          'deleteFlow',
          'deleteNode',
          'renameNode',
          'toggleEnvironmentPlace',
          'toggleIsInitialTransit',
          'setIsSpecial',
          'fireTransition',
          'setInitialToken',
          'setWinningCondition',
          'setFairness',
          'setInhibitorArc'
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
      formatTabTitle: function (tab) {
        if (tab.type === 'errorMessage') {
          return 'Error'
        }
        const typePrettyPrinted = formatJobType(tab.type)
        const shouldIncludeFormula =
          ['MODEL_CHECKING_NET', 'MODEL_CHECKING_RESULT'].includes(tab.type)
        const formulaText = shouldIncludeFormula ? ` for "${tab.jobKey.requestParams.formula}"` : ''
        return typePrettyPrinted.concat(formulaText)
      },
      closeTab: function (tab, side) {
        console.log(`closeTab(${tab.name}, ${side})`)
        // TODO delete this case statement and replace with a component
        switch (side) {
          case 'left':
            this.tabsLeftSide = this.tabsLeftSide.filter(t => t !== tab)
            break
          case 'right':
            if (tab.jobStatus === 'RUNNING') {
              this.cancelJob(tab.jobKey)
            } else {
              this.visibleJobsRightSide =
                this.visibleJobsRightSide.filter(jobKey => !isEqual(jobKey, tab.jobKey))
            }
            break
          default:
            throw new Error('Invalid value for "side" in case statement in closeTab(): ' + side)
        }
      },
      onTabChosen: function (evt) {
        console.log('onTabChosen')
      },
      tabDragStart: function (evt) {
        console.log('tabDragStart')
      },
      tabDragEnd: function (evt, side) {
        console.log('tabDragEnd')
        console.log(evt)
        switch (side) {
          case 'left':
            const newTab = this.tabsLeftSide[evt.newIndex]
            const newTabId = `tab-${newTab.uuid}`
            this.selectedTabLeftSide = newTabId
            break
          case 'right':
            // TODO delete this case statement and replace with a component
            const newTabRight = this.tabsRightSide[evt.newIndex]
            const newTabIdRight = `tab-${newTabRight.uuid}`
            this.selectedTabRightSide = newTabIdRight
            break
          default:
            throw new Error('Invalid value for "side" in case statement in tabDragEnd')
        }
      },
      initializeWebSocket: function (retryAttempts) {
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
          switch (messageParsed.type) {
            case 'serverLogMessage':
              logging.logServerMessage(messageParsed.message, messageParsed.level)
              break
            case 'jobStatusChanged':
              logging.logVerbose('A job\'s status changed.  Updating job list.')
              // TODO Incrementally update list instead of polling and reloading the WHOLE list each time
              this.getListOfJobs()
              // Close the tab of the job if the job has been canceled
              const jobStatus = messageParsed.jobListing.jobStatus
              if (jobStatus === 'CANCELED') {
                this.closeTabIfOpen(messageParsed.jobListing.jobKey)
              }
              break
            case 'jobDeleted':
              // Remove the deleted job from the list of jobListings and close its tab
              this.jobListings = this.jobListings.filter(listing => {
                return !isEqual(messageParsed.jobKey, listing.jobKey)
              })
              this.closeTabIfOpen(messageParsed.jobKey)
              break
            case 'ping':
              logging.logVerbose('Got ping from server.  Sending pong')
              this.socket.send(JSON.stringify({
                type: 'pong'
              }))
              break
            default:
              logging.sendErrorNotification('Got a malformed Websocket message from the server.  See log')
              logging.logObject(message)
          }
        })
        socket.$on('error', event => {
          logging.sendErrorNotification('The websocket connection to the server threw an error.  ' +
            'ADAM\'s log output might not be displayed.')
          console.log('websocket error on next line: ')
          console.log(event)
        })
        socket.$on('close', () => {
          if (retryAttempts < 100) {
            logging.sendErrorNotification('The websocket connection to the server was closed.  ' +
              'ADAM\'s log output might not be displayed.  Attempting to reconnect in 1 second.  ' +
              'Retry attempt #' + retryAttempts)
            setTimeout(() => {
              this.socket = this.initializeWebSocket(retryAttempts + 1)
            }, 1000)
          } else {
            logging.sendErrorNotification('The websocket connection to the server was closed.  ' +
              'ADAM\'s log output might not be displayed.',
              'Reconnect', () => {
                this.socket = this.initializeWebSocket(0)
              })
          }
        })
        socket.$on('open', () => {
          this.updateWebSocketBrowserUuid()
          // TODO Hack to reload the job list whenever the websocket connection is dropped and reacquired
          // TODO Figure out why the connection keeps dropping!!
          this.getListOfJobs() // TODO remove this
          logging.sendSuccessNotification('Established the connection to the server')
        })
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
          const noJobsAreListed = this.jobListings.length === 0
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
        logging.logVerbose('saveAptToFile()')
        const isAptEditorOpen = this.selectedTabLeftSide === 'tab-AptEditorTab'
        if (isAptEditorOpen) {
          saveFileAs(this.apt, this.aptFilename)
        } else {
          this.savePetriGameAsAPT().then(() => saveFileAs(this.apt, this.aptFilename))
        }
      },
      switchToAptEditor: function () {
        this.selectedTabLeftSide = 'tab-AptEditorTab'
      },
      // Send APT to backend and parse it, then display the resulting Petri Game.
      // This is debounced using Underscore: http://underscorejs.org/#debounce
      parseAPTToPetriGame: debounce(function (apt) {
        logging.logVerbose('Sending APT source code to backend.')
        this.restEndpoints.parseApt({
          params: {
            apt: apt,
            netType: this.useModelChecking ? 'petriNetWithTransits' : 'petriGame'
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
      queueJob: function (petriNetId, jobType, jobParams) {
        this.restEndpoints.queueJob({
          petriNetId: petriNetId,
          params: jobParams,
          jobType: jobType
        }).then(response => {
          switch (response.data.status) {
            case 'success':
              // Add a tab corresponding to the newly queued job
              // 'result' is the listing of the job that was queued
              this.openOrAddTab(response.data.result.jobKey)
              this.jobListings.push(response.data.result)
              break
            case 'error':
              if (response.data.errorType === 'JOB_ALREADY_QUEUED') {
                logging.sendErrorNotification(
                  response.data.message,
                  'Show job',
                  () => this.openOrAddTab(response.data.jobKey))
              } else {
                logging.sendErrorNotification(response.data.message)
              }
              break
          }
        })
      },
      calculateModelCheckingNet: function () {
        this.$refs.menubar.deactivate()
        this.queueJob(this.petriGame.uuid, 'MODEL_CHECKING_NET', {
          formula: this.$refs.graphEditorPetriGame[0].ltlFormula
        })
        logging.sendSuccessNotification('Sent a request to get the model checking net')
      },
      checkLtlFormula: function () {
        // this.$refs.menubar.deactivate()
        const formula = this.$refs.graphEditorPetriGame[0].ltlFormula
        this.queueJob(this.petriGame.uuid, 'MODEL_CHECKING_RESULT', {
          formula
        })
        logging.sendSuccessNotification(`Sent a request to check the formula "${formula}"`)
      },
      calculateExistsWinningStrategy: function () {
        this.queueJob(this.petriGame.uuid, 'EXISTS_WINNING_STRATEGY', {})
        logging.sendSuccessNotification('Sent a request to the server to see if there is a winning strategy')
      },
      calculateStrategyBDD: function () {
        this.$refs.menubar.deactivate()
        this.queueJob(this.petriGame.uuid, 'WINNING_STRATEGY', {})
        logging.sendSuccessNotification('Sent request to server to calculate the winning strategy')
      },
      calculateGraphStrategyBDD: function () {
        this.queueJob(this.petriGame.uuid, 'GRAPH_STRATEGY_BDD', {})
        logging.sendSuccessNotification('Sent request to server to calculate the Graph Strategy BDD')
      },
      calculateGraphGameBDD: function (incremental) {
        this.queueJob(this.petriGame.uuid, 'GRAPH_GAME_BDD', {
          incremental
        })
        logging.sendSuccessNotification('Sent request to server to calculate the Graph Game BDD')
      },
      getListOfJobs: function () {
        this.restEndpoints.getListOfJobs()
          .then(response => {
            this.jobListings = response.data.listings
          })
      },
      openOrAddTab: function (jobKey) {
        console.log('openOrAddTab()')
        const alreadyVisibleJob =
          this.visibleJobsRightSide.find(visibleJobKey => isEqual(visibleJobKey, jobKey))
        if (!alreadyVisibleJob) {
          this.visibleJobsRightSide.push(jobKey)
        }
        // Switch to the tab
        // TODO why doesn't the 'selected tab display' update right away?  I thought I fixed this
        //   but apparently it is being strange again.
        const tabId = `tab-${JSON.stringify(jobKey)}`
        this.selectedTabRightSide = tabId
      },
      closeTabIfOpen: function (jobKey) {
        this.visibleJobsRightSide = this.visibleJobsRightSide.filter(
          visibleJobKey => !isEqual(visibleJobKey, jobKey))
      },
      cancelJob: function (jobKey) {
        logging.sendSuccessNotification('Sent request to cancel the job of type ' + jobKey.jobType)
        this.restEndpoints.cancelJob({
          jobKey
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
              this.closeTabIfOpen(jobKey)
              logging.sendSuccessNotification('Cancelled the job and deleted it from the ' +
                'list of jobs.  Note: It might take a little while for the job to actually' +
                ' stop if it was running.  This is a limitation of the libraries used by' +
                ' ADAM.')
          }
        }).then(this.getListOfJobs)
      },
      toggleGraphGameStatePostset: function (stateId, jobKey) {
        this.restEndpoints.toggleGraphGameBDDNodePostset({
          jobKey,
          stateId
        }).then(response => {
        })
      },
      toggleGraphGameStatePreset: function (stateId, jobKey) {
        this.restEndpoints.toggleGraphGameBDDNodePreset({
          jobKey,
          stateId
        }).then(response => {
        })
      },
      onAptEditorInput: function (apt) {
        this.apt = apt
      },
      // Callback function called when the user clicks on a tab and switches to it from a different tab
      onSwitchToTabLeftSide: function (tab) {
        console.log(`onSwitchToTabLeftSide(tab with uuid = ${tab.uuid})`)
        console.log(tab)
        if (`tab-${tab.uuid}` === this.selectedTabLeftSide) {
          console.log('This tab was already selected')
          return
        }
        if (tab.type === 'aptEditor') {
          logging.logVerbose('Switched to APT editor')
          this.savePetriGameAsAPT()
        }
      },
      saveXYCoordinatesOnServer: function () {
        // Our graph editor should give us an object with Node IDs as keys and x,y coordinates as values.
        // We send those x,y coordinates to the server, and the server saves them as annotations
        // into the PetriGame object.
        // TODO Don't use a ref for this.  Put the function inside of here.
        const nodePositions = this.$refs.graphEditorPetriGame[0].getNodeXYCoordinates()
        return this.restEndpoints.updateXYCoordinates({
          petriNetId: this.petriGame.uuid,
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
          petriNetId: this.petriGame.uuid
        }).then(response => {
          return this.withErrorHandling(response, response => {
            return response.data.apt
          })
        })
      },
      // Save xy coordinates on the server and then get the new updated APT back
      savePetriGameAsAPT: function () {
        return this.saveXYCoordinatesOnServer()
          .then(this.getAptOfPetriGame)
          .then(apt => {
            this.apt = apt
          })
      },
      createFlow: function (flowSpec) {
        console.log('processing createFlow event')
        this.restEndpoints.createFlow({
          petriNetId: this.petriGame.uuid,
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
      createTransit: function ({source, transition, postset}) {
        console.log('processing createTransit event')
        this.restEndpoints.createTransit({
          petriNetId: this.petriGame.uuid,
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
          petriNetId: this.petriGame.uuid,
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
          petriNetId: this.petriGame.uuid,
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
          petriNetId: this.petriGame.uuid,
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
          petriNetId: this.petriGame.uuid,
          nodeId: nodeId
        }).then(response => {
          this.withErrorHandling(response, response => {
            this.petriGame.net = response.data.result
          })
        }).catch(() => {
          logging.logError('Network error')
        })
      },
      toggleIsInitialTransit: function (nodeId) {
        console.log('processing toggleIsInitialTransit event')
        this.restEndpoints.toggleIsInitialTransit({
          petriNetId: this.petriGame.uuid,
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
          petriNetId: this.petriGame.uuid,
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
          petriNetId: this.petriGame.uuid,
          transitionId: transitionId
        }).then(response => {
          this.lastPetriGameTransitionFired = {
            id: transitionId,
            successful: response.data.status === 'success',
            timestamp: new Date()
          }
          this.withErrorHandling(response, response => {
            logging.sendSuccessNotification('Fired transition ' + transitionId)
            this.petriGame.net = response.data.result
          })
        })
      },
      setInitialToken: function ({nodeId, tokens}) {
        console.log('processing setInitialToken event')
        this.restEndpoints.setInitialToken({
          petriNetId: this.petriGame.uuid,
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
          petriNetId: this.petriGame.uuid,
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
          petriNetId: this.petriGame.uuid,
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
      setFairness: function ({transitionId, fairness}) {
        logging.logVerbose(`setFairness(${transitionId}, ${fairness})`)
        this.restEndpoints.setFairness({
          petriNetId: this.petriGame.uuid,
          transitionId,
          fairness
        }).then(response => {
          this.withErrorHandling(response, response => {
            this.petriGame.net = response.data.result
          })
        })
      },
      setInhibitorArc: function ({sourceId, targetId, isInhibitorArc}) {
        logging.logVerbose(`setInhibitorArc(${sourceId}, ${targetId}, ${isInhibitorArc})`)
        this.restEndpoints.setInhibitorArc({
          petriNetId: this.petriGame.uuid,
          sourceId,
          targetId,
          isInhibitorArc
        }).then(response => {
          this.withErrorHandling(response, response => {
            this.petriGame.net = response.data.result
          })
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
        this.$refs.graphEditorPetriGame[0].onLoadNewPetriGame()
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
        this.resetQuickAction()
      },
      resetNotification: function () {
        this.notificationMessage = ''
        this.resetQuickAction()
      },
      resetQuickAction: function () {
        this.quickAction = {
          actionName: '',
          action: () => {
          }
        }
      },
      showQuickActionLink: function (actionName, action) {
        this.quickAction = {actionName, action}
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
