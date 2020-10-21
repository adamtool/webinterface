<template>
  <v-app absolute id='app'>
    <!-- Invisible 'input' el needed in order to trigger the browser's file picker -->
    <input id="file-picker" type="file" style="display: none;" v-on:change="onFileSelected"/>

    <!-- The 'file' 'settings' etc. menu at the top of the screen -->
    <MyVueMenuTheme
      style="z-index: 999; display: flex; flex-direction: row; justify-content: space-between;">
      <hsc-menu-bar :style="menuBarStyle" ref="menubar">
        <hsc-menu-bar-item style="width: 60px;" label="File ▾">
          <hsc-menu-item
            :label="useModelChecking ? 'New Petri net with transits' : 'New Petri game'"
            @click.native="createNewEditorNet"/>
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
        <hsc-menu-bar-item
          label="View ▾" style="width: 70px;"
        >
          <hsc-menu-item
            v-model="showLogWindow"
            label="Log Window"
          />
          <hsc-menu-item
            v-model="showJobList"
            label="Job Queue"
          />
          <hsc-menu-item
            v-model="showRightPanelToggle"
            label="Show right panel"
          />
          <hsc-menu-item
            v-model="showPhysicsControls"
            label="Show physics controls"
          />
          <hsc-menu-item
            v-model="showPartitions"
            v-if="!useModelChecking"
            label="Show partitions"
          />
          <hsc-menu-item
            v-model="showNodeLabels"
            label="Show node labels instead of IDs"
          />
        </hsc-menu-bar-item>
        <template v-if="useDistributedSynthesis">
          <hsc-menu-bar-item @click.native="calculateStrategyBDD" label="Solve"
                             style="width: 20px;"/>
          <hsc-menu-bar-item label="Analyze ▾" style="width: 100px;">
            <hsc-menu-item @click.native="calculateExistsWinningStrategy"
                           label="Exists Winning Strategy?"/>
            <hsc-menu-item @click.native="calculateGraphStrategyBDD"
                           label="2-Player Strategy"/>
            <hsc-menu-item @click.native="calculateGraphGameBDD(false)"
                           label="2-Player game (complete)"/>
            <hsc-menu-item @click.native="calculateGraphGameBDD(true)"
                           label="2-Player game (incremental)"/>
          </hsc-menu-bar-item>
        </template>
        <template v-if="useModelChecking">
          <hsc-menu-bar-item @click.native="checkLtlFormula" label="Check" style="width: 20px;"
          />
          <hsc-menu-bar-item label="Reduction ▾" style="width: 110px;">
            <hsc-menu-item @click="calculateModelCheckingNet" label="Petri Net"
            />
            <hsc-menu-item @click="calculateModelCheckingFormula" label="LTL Formula"
            />
          </hsc-menu-bar-item>
        </template>
        <template v-if="useModelChecking">
          <hsc-menu-bar-item
            label="AdamWEB — Model Checker"
            style="text-align: center;"
          />
        </template>
        <template v-if="useDistributedSynthesis">
          <hsc-menu-bar-item
            label="AdamWEB — Synthesizer"
            style="text-align: center;"
          />
        </template>
        <hsc-menu-bar-item style="width: 5px;"
                           label="Help"
                           @click.native="showHelpModal = true; $refs.menubar.deactivate()"
        />
        <hsc-menu-bar-item
          @click.native="showAboutModal = true; $refs.menubar.deactivate()"
          label="About"
          style="width: 20px;"
        />
        <hsc-menu-bar-item
          @click.native="openGithub(); $refs.menubar.deactivate()"
          label="GitHub"
          style="width: 25px;"
        />
        <hsc-menu-bar-item
          label="Home"
          style="width: 5px;"
          @click.native="onClickHome(); $refs.menubar.deactivate()"
        />
      </hsc-menu-bar>
      <v-dialog
        style="display: block;"
        max-width="600"
        v-model="showAboutModal"
        @keydown.esc="showAboutModal = false">
        <v-card>
          <v-card-title
            primary-title
            style="justify-content: space-between;">
            <span>About AdamWEB</span>
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

      <v-dialog
        style="display: block;"
        max-width="600"
        v-model="showHelpModal"
        @keydown.esc="showHelpModal = false">
        <v-card>
          <v-card-title
            primary-title
            style="justify-content: space-between;">
            <span>Help</span>
            <v-icon standard right
                    @click="showHelpModal = false">
              close
            </v-icon>
          </v-card-title>
          <v-card-text>
            <Help
              :useModelChecking="useModelChecking"
            />
          </v-card-text>
        </v-card>
      </v-dialog>
      <!-- <div style="line-height: 34px; font-size: 18px; padding-right: 10px;">
        {{ useModelChecking ? 'AdamWEB - Model Checker' : 'AdamWEB - Synthesizer' }}
      </div>-->
    </MyVueMenuTheme>

    <!-- The main window, split into two resizeable panes using flexbox and splitjs -->
    <div style="display: flex; flex-direction: row; height: calc(100vh - 34.333px); width: 100%; margin-top: 5px;"
         ref="horizontalSplitDiv">
      <div class="flex-column-divider"
           v-on:click="toggleLeftPane"
           v-if="shouldShowRightSide">
        <div :class="isLeftPaneVisible ? 'arrow-left' : 'arrow-right'"></div>
      </div>
      <v-tabs class="tabs-component-full-height"
              :style="splitLeftSideStyle"
              id="splitLeftSide"
              v-model="selectedTabLeftSide">
        <v-tab>{{ useModelChecking ? 'Petri Net with Transits' : 'Petri Game' }}</v-tab>
        <v-tab>Simulator</v-tab>
        <v-tab>APT Editor</v-tab>
        <v-tab-item
          transition="false"
          reverse-transition="false"
        >
          <keep-alive>
            <div style="position: relative; height: 100%; width: 100%;">
              <GraphEditor :graph='editorNet.net'
                           :netType='useModelChecking ? "PETRI_NET_WITH_TRANSITS" : "PETRI_GAME"'
                           :editorNetId='editorNet.uuid'
                           editorMode='Editor'
                           ref='graphEditor'
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
                           v-on:setInitialToken='setInitialToken'
                           v-on:setPartition='setPartition'
                           v-on:setWinningCondition='setWinningCondition'
                           v-on:setFairness='setFairness'
                           v-on:setInhibitorArc='setInhibitorArc'
                           :restEndpoints="restEndpoints"
                           :useModelChecking="useModelChecking"
                           :useDistributedSynthesis="useDistributedSynthesis"
                           :modelCheckingRoutes="modelCheckingRoutes"
                           :showPhysicsControls="showPhysicsControls"
                           :showPartitions="showPartitions"
                           :showNodeLabels="showNodeLabels"
                           :repulsionStrengthDefault="360"
                           :linkStrengthDefault="0.086"
                           :isVisible="isLeftPaneVisible && selectedTabLeftSide === 0"
              />
            </div>
          </keep-alive>
        </v-tab-item>
        <v-tab-item
          transition="false"
          reverse-transition="false"
        >
          <keep-alive>
            <div style="position: relative; height: 100%; width: 100%;">
              <div v-if="simulatorNet.status !== 'netIsPresent'">
                The net in the editor is being copied.
                <div style="white-space: pre-wrap;">simulatorNet: {{
                    JSON.stringify(simulatorNet,
                      null, 2)
                  }}
                </div>
              </div>
              <Simulator
                v-else
                ref="simulator"
                editorMode="Simulator"
                @saveDataFlowPdf="saveSimulatorDataFlowPdf"
                :graph="simulatorNet.net"
                :editorNetId="simulatorNet.uuid"
                :jobKey="simulatorNet.jobKey"
                :cxType="simulatorNet.cxType"
                :cxData="simulatorNet.cxData"
                :netType='useModelChecking ? "PETRI_NET_WITH_TRANSITS" : "PETRI_GAME"'
                :restEndpoints="restEndpoints"
                :useModelChecking="useModelChecking"
                :useDistributedSynthesis="useDistributedSynthesis"
                :showPhysicsControls="showPhysicsControls"
                :showPartitions="showPartitions"
                :showNodeLabels="showNodeLabels"
                :repulsionStrengthDefault="360"
                :linkStrengthDefault="0.086"
                :isVisible="isLeftPaneVisible && selectedTabLeftSide === 1"
              >
                <v-btn
                  style="position: absolute; top: 20px; left: 5px; z-index: 5;"
                  rounded
                  color="blue"
                  @click="copyEditorNetToSimulator"
                >
                  {{ useModelChecking ? 'Load net from editor' : 'Load game from editor' }}
                </v-btn>
              </Simulator>
            </div>
          </keep-alive>
        </v-tab-item>
        <v-tab-item :transition="false"
                    :reverse-transition="false">
          <keep-alive>
            <div style="position: relative; height: 100%; width: 100%;">
              <AptEditor :aptProp='apt'
                         :aptParseStatus='aptParseStatus'
                         :aptParseError='aptParseError'
                         :aptParseErrorLineNumber='aptParseErrorLineNumber'
                         :aptParseErrorColumnNumber='aptParseErrorColumnNumber'
                         @input='onAptEditorInput'/>
            </div>
          </keep-alive>
        </v-tab-item>
      </v-tabs>
      <v-tabs class="tabs-component-full-height" :style="splitRightSideStyle" id="splitRightSide"
              show-arrows
              v-model="selectedTabRightSide">
        <draggable v-model="visibleJobsRightSide" class="v-slide-group__content v-tabs-bar__content"
                   @start="tabDragStart"
                   @end="tabDragEnd"
                   @choose="onTabChosen">
          <!--We include the tab's index in the key so that this component will re-render when
          the tabs' order changes.  That's necessary so that the 'current tab' indicator will
          update appropriately after a drag-drop.-->
          <JobTab v-for="(tab, index) in tabsRightSide"
                  :key="`${index}-${tab.uuid}`"
                  :tab="tab"
                  @closeTab="closeTab(tab)"
          />
        </draggable>
        <v-tabs-items
          v-model="selectedTabRightSide"
        >
          <v-tab-item v-for="tab in tabsRightSide"
                      :key="tab.uuid"
                      :value="`tab-${tab.uuid}`"
                      :transition="false"
                      :reverse-transition="false">
            <keep-alive>
              <JobTabItem
                :tab="tab"
                :showPhysicsControls="showPhysicsControls"
                :showPartitions="showPartitions"
                :showNodeLabels="showNodeLabels"
                :restEndpoints="restEndpoints"
                :useModelChecking="useModelChecking"
                :isTabSelected="shouldShowRightSide && selectedTabRightSide === `tab-${tab.uuid}`"
                @loadEditorNetFromApt="apt => parseAptForEditorNet(apt).then(() => selectedTabLeftSide = 0)"
                @loadCxInSimulator="loadCxInSimulator"
                @saveWitnessesPdf="saveWitnessesPdf"
                @cancelJob="cancelJob"
                @toggleStatePostset="({graphEditorRef, stateId}) => toggleGraphGameStatePostset(graphEditorRef, stateId, tab.jobKey)"
                @toggleStatePreset="({graphEditorRef, stateId}) => toggleGraphGameStatePreset(graphEditorRef, stateId, tab.jobKey)"
              />
            </keep-alive>
          </v-tab-item>
        </v-tabs-items>
      </v-tabs>
    </div>

    <!-- The notification bar at the bottom of the screen -->
    <div :style="`color: ${this.notificationColor}`">
      {{ notificationMessage }}
      <a @click="quickAction.action">
        {{ quickAction.actionName }}
      </a>
    </div>

    <!-- The log window -->
    <hsc-window-style-metal>
      <hsc-window resizable
                  :positionHint="logWindowPositionHint"
                  closeButton
                  :minWidth="logWindowMinWidth"
                  :minHeight="logWindowMinHeight"
                  :height.sync="logWindowHeight"
                  :width.sync="logWindowWidth"
                  :isOpen.sync="showLogWindow"
                  title="Log"
                  style="z-index: 9999">
        <LogViewer
          style="height: inherit; width: inherit;"
        />
      </hsc-window>
    </hsc-window-style-metal>

    <!-- The 'job list' modal dialog -->
    <v-dialog v-model="showJobList"
              :hide-overlay="false"
              :persistent="false"
              :scrollable="true"
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
        <v-card-text
          style="max-height: 70vh;"
        >
          <JobList
            :jobListings="jobListings"
            :useModelChecking="useModelChecking"
            style="background-color: white;"
            @loadJob="openOrAddTab"
            @cancelJob="cancelJob"
            @deleteJob="deleteJob"/>
          <div
            style="padding-top: 15px;">
            <v-expansion-panels>
              <v-expansion-panel>
                <v-expansion-panel-header>
                  More info
                </v-expansion-panel-header>
                <v-expansion-panel-content>
                  <div>
                    The jobs listed here are stored in-memory on the server and will disappear
                    if
                    the server is restarted.
                  </div>
                  <div>
                    You will also lose access to them if you clear the "local
                    storage" of your browser. That's because you can only see jobs that
                    correspond
                    to a randomly generated unique ID that is stored in your local storage.
                  </div>
                  <div>Your unique ID is {{ clientUuid }}.</div>
                  <div>
                    If you use multiple browsers, you can share one unique ID between them in
                    order to have the same list of jobs appear in all of your browsers.
                  </div>
                  <v-text-field
                    v-model="clientUuidEntry"
                    :rules="[validateClientUuid]"
                    label="Other Browser UUID"/>
                  <v-btn
                    @click="saveClientUuid">
                    Use other UUID
                  </v-btn>
                </v-expansion-panel-content>
              </v-expansion-panel>
            </v-expansion-panels>
          </div>
        </v-card-text>
      </v-card>
    </v-dialog>
  </v-app>
</template>


<script>
  import {fakeTabs} from './testData'
  import {aptFileTreeSynthesis, aptFileTreeModelChecking} from './aptExamples'
  import GraphEditor from './components/GraphEditor'
  import Simulator from './components/Simulator'
  import AboutAdamWeb from './components/AboutAdamWeb'
  import Help from './components/Help'
  import LogViewer from './components/LogViewer'
  import JobList from './components/JobList'
  import Vue from 'vue'
  import * as axios from 'axios'
  import {debounce, isEqual} from 'underscore'
  import * as modelCheckingRoutesFactory from './modelCheckingRoutes'

  import Vuetify from 'vuetify'
  import 'vuetify/dist/vuetify.min.css'

  import * as VueMenu from '@hscmap/vue-menu'

  Vue.use(VueMenu)
  import MyVueMenuTheme from './menuStyle'

  import * as VueWindow from '@hscmap/vue-window'

  Vue.use(VueWindow)

  import aptExampleEmptySynthesis from './aptExampleEmptySynthesis.apt'
  import aptExampleEmptyModelChecking from './aptExampleEmptyModelChecking.apt'
  import HscMenuBarDirectory from './components/hsc-menu-bar-directory'

  import makeWebSocket from './logWebSocket'
  import {saveFileAs} from './fileutilities'

  import Split from 'split.js'

  import logging from './logging'
  import AptEditor from './components/AptEditor'
  import JobTab from './components/JobTab'
  import JobTabItem from './components/JobTabItem'

  import {format} from 'date-fns'

  import draggable from 'vuedraggable'

  import {deepCopy, sleep} from './util'

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
      Simulator,
      MyVueMenuTheme,
      LogViewer,
      AptEditor,
      JobList,
      JobTab,
      JobTabItem,
      AboutAdamWeb,
      Help
    },
    created: function () {
      console.log(`AdamWEB App.vue Configuration:
      useDistributedSynthesis: ${this.useDistributedSynthesis}
      useModelChecking: ${this.useModelChecking}
      baseurl: ${this.baseUrl}`)

      // Load our 'clientUuid' which identifies this client to the server
      // If it's not there, generate a new random one.
      if (window.localStorage.getItem('clientUuid') === null) {
        window.localStorage.setItem('clientUuid', uuidv4())
      }
      this.clientUuid = window.localStorage.getItem('clientUuid')
      console.log(`clientUuid: ${this.clientUuid}`)

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

      this.parseAptForEditorNet(this.apt)
      this.getListOfJobs()
    },
    mounted: function () {
      // Initialize draggable, resizable pane
      this.horizontalSplit = this.createHorizontalSplit()
    },
    data: function () {
      return {
        // The contents of the APT editor
        apt: this.useModelChecking ? aptExampleEmptyModelChecking : aptExampleEmptySynthesis,
        aptParseStatus: 'success',
        aptParseError: '',
        aptParseErrorLineNumber: -1,
        aptParseErrorColumnNumber: -1,

        // The net displayed in the graph editor.
        // Corresponds to the class 'EditorNetClient' on the server
        editorNet: {
          net: {
            links: [],
            nodes: []
          },
          uuid: 'abcfakeuuid123',
          initialMarking: {}
        },
        // The net in the simulator.
        // Possible values for 'status' include 'netIsNotPresent', 'netIsPresent', 'copyInProgress'
        // and 'error'.
        // When status === 'netIsPresent', this should have all the same properties as the class
        // 'EditorNetClient' on the server.
        simulatorNet: {status: 'netIsNotPresent'},
        // The following flags show/hide various windows / modal dialogs.
        // They are bound two-way with 'v-model'
        showLogWindow: false,
        showSaveAptModal: false,
        showJobList: false,
        showAboutModal: false,
        showHelpModal: false,
        showRightPanelToggle: true, // In "View" menu, allow hiding the right panel completely
        // The contents of the 'save apt' filename box
        aptFilename: 'apt.txt',

        // The contents of the 'notification bar' at the bottom of the window, e.g. error messages
        // and warnings and other feedback
        notificationMessage: '',
        notificationColor: '',
        // Some notifications may contain a link that the user can click to quickly do a certain action.
        quickAction: {
          actionName: '',
          action: () => {
          }
        },

        // These are global flags which are applied to all graph viewer instances in all tabs
        showPhysicsControls: false,
        showPartitions: false,
        showNodeLabels: false,

        // This uuid is a key of the 'Map<UUID, UserContext>' on the server.
        // Each UserContext object represents a separate job queue and list of queued/completed jobs.
        // The client generates its own uuid when the app is loaded for the first time.
        clientUuid: 'client uuid not yet loaded. (Should have been initialized in mounted hook)',
        // The contents of the text field used to enter a new UUID
        clientUuidEntry: '',
        // the list of enqueued/running/finished jobs seen by this client.
        // Corresponds to the output of UserContext::getJobList on the server.
        jobListings: [],

        // The main window is split into two resizable panes, left and right, using Split.js.
        // See "API" section on https://nathancahill.github.io/Split.js/
        // The instance returned by the 'Split' function of Split.js.
        horizontalSplit: undefined,
        // The relative percentage sizes of the two sides of the screen
        horizontalSplitSizes: [50, 50],
        // True iff the left-hand pane of the split (Petri Game/Simulator/APT Editor) is snapped shut
        isLeftPaneVisible: true,
        // The left-hand pane automatically snaps shut if it is shrunk to this percentage of its
        // maximum width
        leftPaneMinWidth: 7.65,

        // The tabs on the left and right sides of the screen are displayed using Vuetify's
        // 'v-tabs' component.
        selectedTabLeftSide: 0,
        selectedTabRightSide: '',
        // Which jobs' tabs are open. Equivalent to List<JobKey> on the server
        visibleJobsRightSide: [],

        // Initial log window size
        logWindowWidth: window.innerWidth * 0.7,
        logWindowHeight: window.innerHeight * 0.6,
        // Minimum log window size
        logWindowMinWidth: 700,
        logWindowMinHeight: 200
      }
    },
    watch: {
      // When our client UUID is changed, we should reload the list of jobs and tell the server
      // we want to subscribe to job status updates and ADAM's logs corresponding to our new UUID
      clientUuid: function () {
        this.getListOfJobs()
        if (this.socket.isReady()) {
          this.updateWebSocketClientUuid()
        }
      },
      // When we open the modal dialog, we should reload the list of jobs
      showJobList: function () {
        if (this.showJobList) {
          this.getListOfJobs()
        }
      },
      apt: function (apt) {
        this.parseAptForEditorNet(apt)
      },
      aptParseStatus: function (status) {
        if (status === 'error') {
          this.selectedTabLeftSide = 2 // APT Editor
        }
      },
      selectedTabLeftSide: function (tabIndex) {
        if (tabIndex === 2) { // APT Editor
          this.saveEditorNetAsAPT()
        } else if (tabIndex === 1) { // Simulator
          const {status} = this.simulatorNet
          if (status !== 'netIsPresent' && status !== 'copyInProgress') {
            this.copyEditorNetToSimulator()
          }
        }
      },
      shouldShowRightSide: function () {
        if (!this.shouldShowRightSide) {
          // Make sure that the left side is open if the right side is closed.
          // (It is strange if everything is closed and you just have a big blank screen.)
          this.horizontalSplitSizes = this.horizontalSplit.getSizes()
          this.horizontalSplit.setSizes([100, 0])
          this.isLeftPaneVisible = true
        } else {
          // Restore the sizes that were there last time
          this.horizontalSplit.setSizes(this.horizontalSplitSizes)
        }
      }
    },
    computed: {
      logWindowPositionHint: function () {
        const x = (window.innerWidth - this.logWindowWidth) / 2
        const y = (window.innerHeight - this.logWindowHeight) / 2.5
        return `${x} / ${y}`
      },
      tabsRightSide: function () {
        console.log('updated tabsRightSide')
        return this.visibleJobsRightSide.map((jobKey) => jobKeyToTab(this.jobListings, jobKey))

        function jobKeyToTab(jobListings, jobKey) {
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
      menuBarStyle: function () {
        const vuetifySidebarPadding = this.$vuetify.application.left
        return ` width:100%; border-radius: 0 0 4pt 0; padding-left: ${vuetifySidebarPadding + 10}px;
                 box-shadow: rgba(0, 0, 0, 0.3) 0px 2pt 4pt;`
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
        return this.tabsRightSide.length !== 0 && this.showRightPanelToggle
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
          'getAptOfEditorNet',
          'updateXYCoordinates',
          'parseApt',
          'copyEditorNet',
          'loadCxInSimulator',
          'saveJobAsApt',
          'saveJobAsPnml',
          'insertPlace',
          'createFlow',
          'createTransit',
          'deleteFlow',
          'deleteNode',
          'renameNode',
          'toggleEnvironmentPlace',
          'toggleIsInitialTransit',
          'setIsSpecial',
          'fireTransitionEditor',
          'fireTransitionJob',
          'setInitialToken',
          'setPartition',
          'setWinningCondition',
          'setFairness',
          'setInhibitorArc'
        ]
        const funs = {}
        endpoints.forEach(endpointName => {
          funs[endpointName] = (options) => {
            return axios.post(this.baseUrl + '/' + endpointName, {
              ...options,
              clientUuid: this.clientUuid
            })
          }
        })
        return funs
      },
      // TODO #301 Refactor to be the same as restEndpoints
      //  There are different inconsistent interfaces for different routes because I have
      // experimented, trying to find out the cleanest way to implement a RPC mechanism like this,
      // and I have not yet cleaned it all up yet. -ann
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
      onClickHome: function () {
        if (window.confirm('Go back to the main menu?  (The net in the editor will be lost.)')) {
          this.$router.push('/')
        }
      },
      openGithub: function () {
        window.open('https://github.com/adamtool/webinterface')
      },
      copyEditorNetToSimulator: function () {
        this.simulatorNet = {status: 'copyInProgress'}
        this.copyEditorNet()
          .then(editorNetCopy => {
            this.simulatorNet = {
              status: 'netIsPresent',
              ...editorNetCopy
            }
            // The ref will be undefined until the Simulator component is rendered by Vue
            Vue.nextTick(() => this.$refs.simulator.resetSimulation())
          }).catch(error => {
          logging.sendErrorNotification(error)
          this.simulatorNet = {
            status: 'error',
            error: error.toString()
          }
        })
      },
      saveWitnessesPdf: function ({jobKey}) {
        const url =
          `${this.baseUrl}/saveWitnessesPdf?jobKey=${JSON.stringify(jobKey)}&clientUuid=${this.clientUuid}`
        window.open(url)
      },
      saveSimulatorDataFlowPdf: function ({jobKey, editorNetId, firingSequence}) {
        let url =
          `${this.baseUrl}/saveSimulatorDataFlowPdf?clientUuid=${this.clientUuid}&firingSequence=${JSON.stringify(firingSequence)}`
        if (editorNetId) {
          url = url + `&editorNetId=${editorNetId}`
        } else if (jobKey) {
          url = url + `&jobKey=${JSON.stringify(jobKey)}`
        }
        window.open(url)
      },
      loadCxInSimulator: function ({jobKey, cxType}) {
        this.simulatorNet = {status: 'copyInProgress'}
        this.selectedTabLeftSide = 1 // Switch to simulator tab
        const promise = this.restEndpoints.loadCxInSimulator({
          params: {
            jobKey,
            cxType
          }
        }).then(response => {
          switch (response.data.status) {
            case 'success':
              const {net, loopPoint, historyStack} = response.data
              // Option<{index: Int, id: String}>
              const transitionFailed = response.data.transitionFailed
              this.simulatorNet = {
                status: 'netIsPresent',
                net,
                jobKey,
                cxType,
                cxData: {
                  loopPoint, // int
                  historyStack // List<SimulationHistoryState>  (See SimulationHistoryState.java)
                }
              }
              Vue.nextTick(() => {
                this.$refs.simulator.resetSimulation()
                if (transitionFailed) {
                  const {index, id, remainingTransitions} = transitionFailed
                  logging.sendErrorNotification(`The transition ${id} at index ${index} of the firing
                   sequence could not be fired. Only the sequence up to that point will be loaded.
                   The rest of the firing sequence follows: [${remainingTransitions.join(',')}] `)
                }
              })
              break
            case 'error':
              throw new Error(response.data.message)
              break
            default:
              throw new Error('Malformed response from server')
          }
        }).catch(error => {
          console.error(error)
          this.simulatorNet = {
            status: 'error',
            error: error.toString()
          }
          logging.sendErrorNotification(error.toString())
        })
      },
      initializeWebSocket: function (retryAttempts) {
        // Open a websocket to receive server-side log output and job status updates
        let socket = null

        // Send a heartbeat message to keep the websocket alive
        function sendHeartbeat() {
          socket.send(JSON.stringify({
            type: 'heartbeat'
          }))
        }

        let heartbeatInterval = null
        try {
          socket = makeWebSocket(this.webSocketUrl)
        } catch (exception) {
          const errorMessage = 'An exception was thrown when opening the websocket connection to the server.  ' +
            'Server log messages may not be displayed.  Exception: ' + exception.message
          logging.sendErrorNotification(errorMessage)
          throw new Error(errorMessage)
        }
        socket.$on('message', message => {
          const messageParsed = JSON.parse(message)
          switch (messageParsed.type) {
            case 'serverLogMessage':
              logging.logServerMessage(messageParsed.message, messageParsed.level)
              break
            case 'jobStatusChanged':
              console.log('jobStatusChanged.  Status: ' + messageParsed.jobListing.jobStatus)
              // Incrementally update the job list
              const existingJobIndex = this.jobListings.findIndex(
                listing => isEqual(listing.jobKey, messageParsed.jobListing.jobKey)
              )
              if (existingJobIndex === -1) {
                this.jobListings.push(messageParsed.jobListing)
              } else {
                console.log('splicing job status')
                this.jobListings.splice(existingJobIndex, 1, messageParsed.jobListing)
              }
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
          clearInterval(heartbeatInterval) // Stop sending heartbeats on this websocket
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
          // Reload the job list when the socket connection to the server is established/reestablished
          this.updateWebSocketClientUuid()
          this.getListOfJobs()
          logging.sendSuccessNotification('Established the connection to the server')
          heartbeatInterval = setInterval(sendHeartbeat, 30000) // Send a heartbeat every 30s
        })
        return socket
      },
      // Tell the server our clientUuid so that it will send us ADAM's log output for our jobs
      updateWebSocketClientUuid: function () {
        // Make sure we get notifications from the server corresponding to our unique session ID
        this.socket.send(JSON.stringify({
          clientUuid: this.clientUuid
        }))
      },
      saveClientUuid: function () {
        if (this.clientUuidEntry === this.clientUuid) {
          return
        }
        if (this.validateClientUuid(this.clientUuidEntry) === true) {
          const noJobsAreListed = this.jobListings.length === 0
          if (noJobsAreListed || confirm(
            'Changing your browser\'s UUID will cause your current list of jobs to disappear.  ' +
            'The only way to get them back is to re-enter your current UUID (' + this.clientUuid +
            ').  Are you sure?')) {
            this.clientUuid = this.clientUuidEntry
            window.localStorage.setItem('clientUuid', this.clientUuid)
          }
        }
      },
      // Validate whether the given string represents a uuidv4.
      validateClientUuid: function (uuidString) {
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
      // Create a new empty net in the editor
      createNewEditorNet: function () {
        const apt = this.useModelChecking ? aptExampleEmptyModelChecking : aptExampleEmptySynthesis
        this.onAptExampleSelected(apt)
        const whatDoYouCallIt = this.useModelChecking ? 'Petri net with transits' : 'Petri game'
        logging.sendSuccessNotification('Loaded a new empty ' + whatDoYouCallIt)
      },
      closeTab: function (tab) {
        console.log(`closeTab(${tab.name})`)
        if (tab.jobStatus === 'RUNNING') {
          this.cancelJob(tab.jobKey)
        } else {
          this.visibleJobsRightSide =
            this.visibleJobsRightSide.filter(jobKey => !isEqual(jobKey, tab.jobKey))
        }
      },
      onTabChosen: function (evt) {
        console.log('onTabChosen')
      },
      tabDragStart: function (evt) {
        console.log('tabDragStart')
      },
      tabDragEnd: function (evt) {
        console.log('tabDragEnd')
        console.log(evt)
        const newTabRight = this.tabsRightSide[evt.newIndex]
        const newTabIdRight = `tab-${newTabRight.uuid}`
        this.selectedTabRightSide = newTabIdRight
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
            // Snap left side shut
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
              logging.logVerbose('expanding left pane')
              this.isLeftPaneVisible = true
            }

            // Snap right side shut
            const rightPaneWidth = split.getSizes()[1]
            const rightPaneShouldCollapse = rightPaneWidth <= this.leftPaneMinWidth
            if (rightPaneShouldCollapse && this.showRightPanelToggle) {
              logging.logVerbose('collapsing right pane')
              this.showRightPanelToggle = false
            }
            if (rightPaneShouldCollapse) {
              split.setSizes([100, 0])
            }
            if (!rightPaneShouldCollapse && !this.showRightPanelToggle) {
              logging.logVerbose('expanding right pane')
              this.showRightPanelToggle = true
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
        // Reset the file picker so that onchange will fire again if the user reloads the same file
        // (see https://stackoverflow.com/a/28274454)
        changeEvent.target.value = null
        logging.logObject(file)
        const reader = new FileReader()
        reader.onloadend = () => {
          // TODO #299 Note that we do not verify that the file is reasonable (i.e. plain text, not
          //  a binary or other weird file).
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
        const isAptEditorOpen = this.visibleTabContentsLeftSide === 'aptEditor'
        if (isAptEditorOpen) {
          saveFileAs(this.apt, this.aptFilename)
        } else {
          this.saveEditorNetAsAPT().then(() => saveFileAs(this.apt, this.aptFilename))
        }
      },
      /**
       * Send APT to the server to be parsed, then update the net displayed in the editor.
       * This is debounced using Underscore: http://underscorejs.org/#debounce
       * @Returns {Promise<void>}
       */
      parseAptForEditorNet: debounce(function (apt) {
        logging.logVerbose('Sending APT to server.')
        const promise = this.restEndpoints.parseApt({
          params: {
            apt: apt,
            netType: this.useModelChecking ? 'PETRI_NET_WITH_TRANSITS' : 'PETRI_GAME'
          }
        }).then(response => {
          switch (response.data.status) {
            case 'success':
              logging.logVerbose('Successfully parsed APT.')
              logging.logObject(response)
              this.editorNet = response.data.editorNet
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
        }).catch(error => {
          logging.sendErrorNotification(
            'Network error when trying to parse APT: ' + error.toString())
        })
        this.aptParseStatus = 'running'
        return promise
      }, 200),
      /**
       * @returns Basically a Promise<EditorNetClient>
       */
      copyEditorNet: function () {
        // Copy the existing clientside representation of the editor net, including node positions
        const editorNetCopy = deepCopy(this.editorNet)
        editorNetCopy.net.nodePositions = this.$refs.graphEditor.getNodeXYCoordinates()
        // Ask the server to duplicate the editor net object and save it under a new UUID
        return this.restEndpoints.copyEditorNet({
          editorNetId: this.editorNet.uuid
        }).then(response => {
          if (response.data.status === 'success') {
            return {
              net: editorNetCopy.net,
              initialMarking: editorNetCopy.initialMarking,
              uuid: response.data.uuid
            }
          } else {
            console.error(response)
            throw new Error('Got a bad response from the server. Please check the browser\'s ' +
              'console output for details')
          }
        })
      },
      queueJob: function (editorNetId, jobType, jobParams) {
        this.restEndpoints.queueJob({
          editorNetId: editorNetId,
          params: jobParams,
          jobType: jobType
        }).then(response => {
          switch (response.data.status) {
            case 'success':
              // 'response.data.result' is the listing of the job that was queued
              // Only add it to the list if its not already there
              // (There could be a race condition with the websocket's jobStatusChanged message)
              if (!this.jobListings.find(listing =>
                isEqual(listing.jobKey, response.data.result.jobKey))) {
                this.jobListings.push(response.data.result)
              }
              // Add a tab corresponding to the newly queued job
              this.openOrAddTab(response.data.result.jobKey)
              break
            case 'error':
              if (response.data.errorType === 'JOB_ALREADY_QUEUED') {
                logging.sendErrorNotification(
                  'The requested job has already been queued.  Automatically switching to tab')
                this.openOrAddTab(response.data.jobKey)
              } else {
                logging.sendErrorNotification(response.data.message)
              }
              break
          }
        })
      },
      calculateModelCheckingNet: function () {
        this.$refs.menubar.deactivate()
        const formula = this.$refs.graphEditor.ltlFormula
        if (formula === '') {
          this.setLtlParseError('Please enter a formula to check.')
          return
        }
        this.queueJob(this.editorNet.uuid, 'MODEL_CHECKING_NET', {
          formula
        })
        logging.sendSuccessNotification('Sent a request to get the model checking net')
      },
      calculateModelCheckingFormula: function () {
        const formula = this.$refs.graphEditor.ltlFormula
        if (formula === '') {
          this.setLtlParseError('Please enter a formula to check.')
          return
        }
        this.queueJob(this.editorNet.uuid, 'MODEL_CHECKING_FORMULA', {
          formula
        })
        logging.sendSuccessNotification('Sent a request to get the LTL formula of the reduction')
      },
      checkLtlFormula: function () {
        this.$refs.menubar.deactivate()
        const formula = this.$refs.graphEditor.ltlFormula
        if (formula === '') {
          this.setLtlParseError('Please enter a formula to check.')
          return
        }
        this.queueJob(this.editorNet.uuid, 'MODEL_CHECKING_RESULT', {
          formula
        })
        logging.sendSuccessNotification(`Sent a request to check the formula "${formula}"`)
      },
      setLtlParseError: function (message) {
        // NOTE: This is currently kind of a mess with these variables being accessed and written to
        //  both here and inside of the GraphEditor component.  I think it might make sense to
        //  put them into a central store somehow. -ann
        const graphEditorRef = this.$refs.graphEditor
        graphEditorRef.ltlParseStatus = 'error'
        // clear the error and then set it again in the next tick, so that the 'v-text-field'
        // component will do its "error" wiggle animation again if you cause another error after it
        // was already in an error state
        graphEditorRef.ltlParseErrors = []
        Vue.nextTick(() => {
          this.$refs.graphEditor.ltlParseErrors = [message]
        })
      },
      calculateExistsWinningStrategy: function () {
        this.queueJob(this.editorNet.uuid, 'EXISTS_WINNING_STRATEGY', {})
        logging.sendSuccessNotification('Sent a request to the server to see if there is a winning strategy')
      },
      calculateStrategyBDD: function () {
        this.$refs.menubar.deactivate()
        this.queueJob(this.editorNet.uuid, 'WINNING_STRATEGY', {})
        logging.sendSuccessNotification('Sent request to server to calculate the winning strategy')
      },
      calculateGraphStrategyBDD: function () {
        this.queueJob(this.editorNet.uuid, 'GRAPH_STRATEGY_BDD', {})
        logging.sendSuccessNotification('Sent request to server to calculate the Graph Strategy BDD')
      },
      calculateGraphGameBDD: function (incremental) {
        this.queueJob(this.editorNet.uuid, 'GRAPH_GAME_BDD', {
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
        const tabId = `tab-${JSON.stringify(jobKey)}`
        this.selectedTabRightSide = tabId
        // Ensure that the right-hand panel is open
        this.showRightPanelToggle = true
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
        })
      },
      toggleGraphGameStatePostset: function (graphEditorRef, stateId, jobKey) {
        this.restEndpoints.toggleGraphGameBDDNodePostset({
          jobKey,
          stateId
        }).then(response => {
          // The view will update itself reactively when a response arrives over the websocket
          // via 'jobStatusChanged'.
          // But we manually raise the physics simulation alpha so that the (possibly) newly
          // visible states will "pop out" appropriately.
          graphEditorRef.physicsSimulation.alpha(0.5).restart()
        })
      },
      toggleGraphGameStatePreset: function (graphEditorRef, stateId, jobKey) {
        this.restEndpoints.toggleGraphGameBDDNodePreset({
          jobKey,
          stateId
        }).then(response => {
          // The view will update itself reactively when a response arrives over the websocket
          // via 'jobStatusChanged'.
          // But we manually raise the physics simulation alpha so that the (possibly) newly
          // visible states will "pop out" appropriately.
          graphEditorRef.physicsSimulation.alpha(0.5).restart()
        })
      },
      onAptEditorInput: function (apt) {
        this.apt = apt
      },
      saveXYCoordinatesOnServer: function () {
        // Our graph editor should give us an object with Node IDs as keys and x,y coordinates as values.
        // We send those x,y coordinates to the server, and the server saves them as annotations
        // into the PetriNetWithTransits/PetriGame on the server.
        const nodePositions = this.$refs.graphEditor.getNodeXYCoordinatesFixed()
        return this.restEndpoints.updateXYCoordinates({
          editorNetId: this.editorNet.uuid,
          nodeXYCoordinateAnnotations: nodePositions
        }).then(response => {
          return this.withErrorHandling(response, responseSuccess => {
            // It was successful, we don't have to do anything
            logging.logVerbose('saveXYCoordinatesOnServer was successful')
          })
        }, reason => {
          // This function gets called if the promise is rejected (i.e. the http request failed)
          logging.logError('saveXYCoordinatesOnServer(): An error occurred. ' + reason)
          throw new Error(reason)
        })
      },
      // Return a promise with the return value of the new apt
      getAptOfEditorNet: function () {
        return this.restEndpoints.getAptOfEditorNet({
          editorNetId: this.editorNet.uuid
        }).then(response => {
          return this.withErrorHandling(response, response => {
            return response.data.apt
          })
        })
      },
      // Save xy coordinates on the server and then get the new updated APT back
      saveEditorNetAsAPT: function () {
        // TODO #306 Indicate visually that these requests are in progress.  Consider disabling the
        // APT editor text box and graying it out until the APT has been retrieved
        return this.saveXYCoordinatesOnServer()
          .then(this.getAptOfEditorNet)
          .then(apt => {
            this.apt = apt
          })
      },
      createFlow: function (flowSpec) {
        console.log('processing createFlow event')
        this.restEndpoints.createFlow({
          editorNetId: this.editorNet.uuid,
          source: flowSpec.source,
          destination: flowSpec.destination
        }).then(response => {
          this.withErrorHandling(response, response => {
            this.editorNet.net = response.data.result
          })
        }).catch(() => {
          logging.logError('Network error')
        })
      },
      createTransit: function ({source, transition, postset}) {
        console.log('processing createTransit event')
        this.restEndpoints.createTransit({
          editorNetId: this.editorNet.uuid,
          source,
          transition,
          postset
        }).then(response => {
          this.withErrorHandling(response, response => {
            this.editorNet.net = response.data.result
          })
        }).catch(() => {
          logging.logError('Network error')
        })
      },
      deleteFlow: function ({sourceId, targetId}) {
        console.log('processing deleteFlow event')
        this.restEndpoints.deleteFlow({
          editorNetId: this.editorNet.uuid,
          sourceId,
          targetId
        }).then(response => {
          this.withErrorHandling(response, response => {
            this.editorNet.net = response.data.result
          })
        }).catch(() => {
          logging.logError('Network error')
        })
      },
      deleteNode: function (nodeId) {
        console.log('processing deleteNode event for node id ' + nodeId)
        this.restEndpoints.deleteNode({
          editorNetId: this.editorNet.uuid,
          nodeId: nodeId
        }).then(response => {
          this.withErrorHandling(response, response => {
            this.editorNet.net = response.data.result
          })
        }).catch(() => {
          logging.logError('Network error')
        })
      },
      renameNode: function ({xOld, yOld, idOld, idNew}) {
        console.log('processing renameNode event')
        console.log(`renaming node '${idOld}' to '${idNew}'`)
        this.restEndpoints.renameNode({
          editorNetId: this.editorNet.uuid,
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
            this.editorNet.net = newNet
          })
        }).catch(() => {
          logging.logError('Network error')
        })
      },
      toggleEnvironmentPlace: function (nodeId) {
        console.log('processing toggleEnvironmentPlace event')
        this.restEndpoints.toggleEnvironmentPlace({
          editorNetId: this.editorNet.uuid,
          nodeId: nodeId
        }).then(response => {
          this.withErrorHandling(response, response => {
            this.editorNet.net = response.data.result
          })
        }).catch(() => {
          logging.logError('Network error')
        })
      },
      toggleIsInitialTransit: function (nodeId) {
        console.log('processing toggleIsInitialTransit event')
        this.restEndpoints.toggleIsInitialTransit({
          editorNetId: this.editorNet.uuid,
          nodeId: nodeId
        }).then(response => {
          this.withErrorHandling(response, response => {
            this.editorNet.net = response.data.result
          })
        }).catch(() => {
          logging.logError('Network error')
        })
      },
      setIsSpecial: function ({nodeId, newSpecialValue}) {
        console.log('processing setIsSpecial event')
        this.restEndpoints.setIsSpecial({
          editorNetId: this.editorNet.uuid,
          nodeId,
          newSpecialValue
        }).then(response => {
          this.withErrorHandling(response, response => {
            this.editorNet.net = response.data.result
          })
        }).catch(() => {
          logging.logError('Network error')
        })
      },
      setInitialToken: function ({nodeId, tokens}) {
        console.log('processing setInitialToken event')
        this.restEndpoints.setInitialToken({
          editorNetId: this.editorNet.uuid,
          nodeId: nodeId,
          tokens: tokens
        }).then(response => {
          this.withErrorHandling(response, response => {
            this.editorNet.net = response.data.result
          })
        }).catch(() => {
          logging.logError('Network error')
        })
      },
      setPartition: function ({nodeId, partition}) {
        console.log('processing setPartitoin event')
        this.restEndpoints.setPartition({
          editorNetId: this.editorNet.uuid,
          nodeId,
          partition
        }).then(response => {
          this.withErrorHandling(response, response => {
            this.editorNet.net = response.data.result
          })
        }).catch(() => {
          logging.logError('Network error')
        })
      },
      setWinningCondition: function (winningCondition) {
        this.restEndpoints.setWinningCondition({
          editorNetId: this.editorNet.uuid,
          winningCondition: winningCondition
        }).then(response => {
          this.withErrorHandling(response, response => {
            this.editorNet.net = response.data.result
          })
        }).catch(() => {
          logging.logError('Network error')
        })
      },
      insertNode: function (nodeSpec) {
        console.log('processing insertNode event')
        this.restEndpoints.insertPlace({
          editorNetId: this.editorNet.uuid,
          nodeType: nodeSpec.type,
          x: nodeSpec.x,
          y: nodeSpec.y
        }).then(response => {
          this.withErrorHandling(response, response => {
            // TODO #288 Do not send the whole net back and forth, but rather a 'diff' of the change
            // (or a simple acknowledgement. The diff could be calculated on the client.)
            this.editorNet.net = response.data.result
          })
        }).catch(() => {
          logging.logError('Network error')
        })
      },
      setFairness: function ({transitionId, fairness}) {
        logging.logVerbose(`setFairness(${transitionId}, ${fairness})`)
        this.restEndpoints.setFairness({
          editorNetId: this.editorNet.uuid,
          transitionId,
          fairness
        }).then(response => {
          this.withErrorHandling(response, response => {
            this.editorNet.net = response.data.result
          })
        })
      },
      setInhibitorArc: function ({sourceId, targetId, isInhibitorArc}) {
        logging.logVerbose(`setInhibitorArc(${sourceId}, ${targetId}, ${isInhibitorArc})`)
        this.restEndpoints.setInhibitorArc({
          editorNetId: this.editorNet.uuid,
          sourceId,
          targetId,
          isInhibitorArc
        }).then(response => {
          this.withErrorHandling(response, response => {
            this.editorNet.net = response.data.result
          })
        })
      },
      onDragDropEnd: function (graph) {
        this.saveXYCoordinatesOnServer()
      },
      onAptExampleSelected: function (apt) {
        // Let the Graph Editor know that a new petri game has been loaded.
        // This needs to be handled differently than an incremental edit to an already loaded
        // Petri Game, because when we load a new APT file, we want all of the nodes' positions
        // to be reset.
        this.$refs.graphEditor.onLoadNewNet()
        this.apt = apt
        this.parseAptForEditorNet(apt).then(() => {
          if (this.selectedTabLeftSide === 1) {  // If simulator is open
            this.selectedTabLeftSide = 0 // Switch to editor to show the newly loaded net
          }
        })
        this.isLeftPaneVisible = true
      },
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
  /*Text inside of v-cards should be pure black.*/
  .theme--light.v-card .v-card__subtitle, .theme--light.v-card > .v-card__text {
    color: #000000;
  }

  /*Inactive tab text should not be too hard to read*/
  .theme--light.v-tabs > .v-tabs-bar .v-tab--disabled,
  .theme--light.v-tabs > .v-tabs-bar .v-tab:not(.v-tab--active),
  .theme--light.v-tabs > .v-tabs-bar .v-tab:not(.v-tab--active) > .v-icon {
    color: rgba(0, 0, 0, .7);
  }

  /*Placeholder text of v-input should not be too faint to read*/
  .theme--light.v-input input::placeholder, .theme--light.v-input textarea::placeholder {
    color: rgba(0, 0, 0, .7);
  }

  .theme--light.v-btn.v-btn--icon {
    color: rgba(0, 0, 0, .7);
  }

  html {
    overflow-y: auto;
  }

  @font-face {
    font-family: 'Material Icons';
    font-style: normal;
    font-weight: 400;
    src: local('Material Icons'), local('MaterialIcons-Regular'),
    url('./assets/fonts/MaterialIcons-Regular.woff2') format('woff2');
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
    color: #000000;
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

  .tabs-component-full-height > .v-tabs-bar {
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
