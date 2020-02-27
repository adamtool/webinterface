<template>
  <!--The attribute tabIndex is here to allow the div to receive keyboard focus.-->
  <div class="graph-editor" :id="rootElementId" ref="rootElement" :tabIndex="-1">
    <!--Physics controls toolbar.  Normally hidden.-->
    <div
      style="position: absolute;
      bottom: 0px;
      width: 100%;
      padding-right: 20px;
      z-index: 6;
      background-color: #fafafa"
    >
      <div class="graph-editor-toolbar" v-if="shouldShowPhysicsControls">
        <div>Repulsion Strength</div>
        <input type="range" min="30" max="1000" step="1"
               class="forceStrengthSlider"
               v-model="repulsionStrength">
        <div class="forceStrengthNumber">{{repulsionStrength}}</div>
        <div>Link strength</div>
        <input type="range" min="0" max="0.2" step="0.001"
               class="forceStrengthSlider"
               v-model="linkStrength">
        <div class="forceStrengthNumber">{{linkStrength}}</div>
        <div>Gravity strength</div>
        <input type="range" min="0" max="800" step="1"
               class="forceStrengthSlider"
               v-model="gravityStrength">
        <div class="forceStrengthNumber">{{gravityStrength}}</div>
      </div>
    </div>

    <!--TODO Provide visual feedback when HTTP request is in progress, similar to APT editor-->
    <v-container fluid
                 style="
                 z-index: 5;
                 position: absolute;
                 padding-top: 5px;"
    >
      <v-layout justify-center>
        <template v-if="useModelChecking">
          <v-flex xs3>
            <v-select
              v-if="editorMode === 'Editor'"
              v-model="selectedWinningCondition"
              :items="winningConditions"
              label="Condition"/>
          </v-flex>
          <v-flex xs3 sm7>
            <v-text-field
              style="flex: 1 1 auto;"
              :disabled="selectedWinningCondition !== 'LTL'"
              v-if="editorMode === 'Editor' && useModelChecking"
              v-model="ltlFormula"
              :prepend-inner-icon="ltlParseStatusIcon"
              :error-messages="ltlParseErrors"
              placeholder="Enter an LTL or Flow-LTL formula here"
              label="Formula"/>
          </v-flex>
        </template>
        <template v-else>
          <v-flex xs6 sm5 offset-sm1 md4 offset-md2>
            <v-select
              v-if="editorMode === 'Editor'"
              v-model="selectedWinningCondition"
              :items="winningConditions"
              label="Winning Condition"/>
          </v-flex>
          <v-flex xs6 sm1 md2></v-flex>
        </template>
      </v-layout>
    </v-container>

    <ToolPicker
      style="position: absolute; top: 75px; z-index: 5;
      background: #ffffffee;
      padding: 12px;
      border-radius: 40px;"
      :paddingWithinParentElement="this.shouldShowPhysicsControls ? 250 : 200"
      :selectedTool="this.selectedTool"
      @onPickTool="tool => this.selectedTool = tool"
      :tools="this.toolPickerItems"/>

    <v-card
      v-if="editorMode === 'Simulator'"
      style="position: absolute; top: 75px; right: 5px; bottom: 10px; z-index: 5;
             padding: 6px; padding-top: 6px; padding-bottom: 6px; border-radius: 30px;"
      class="d-flex flex-column"
    >
      <v-card-title class="flex-grow-0 flex-shrink-0">
        Simulation History
      </v-card-title>
      <v-card-text>Current index:
        {{ gameSimulationHistory ? gameSimulationHistory.currentIndex : undefined }}
      </v-card-text>
      <v-list dense
              class="overflow-y-auto"
              style="padding-top: 0;"
      >
        <v-list-item-group
          v-model="gameSimulationHistory ? gameSimulationHistory.currentIndex : {index: 0}.index"
        >
          <v-list-item
            v-for="(historyState, i) in visibleSimulationHistory.stack"
            :key="i"
          >
            <v-list-item-content>
              <v-list-item-title v-text="historyState.transitionFired"></v-list-item-title>
            </v-list-item-content>
          </v-list-item>
        </v-list-item-group>
      </v-list>
    </v-card>

    <v-tooltip bottom>
      <template v-slot:activator="{ on }">
        <v-btn
          icon
          color="blue"
          large
          light
          style="position: absolute; right: 20px; top: 20px; z-index: 5;"
          @click="saveGraph"
          v-on="on">
          <v-icon>save</v-icon>
        </v-btn>
      </template>
      Save as SVG
    </v-tooltip>

    <v-tooltip bottom
               v-if="this.gameSimulationHistory && this.editorMode === 'Simulator'"
    >
      <template v-slot:activator="{ on }">
        <v-btn
          color="blue"
          large
          light
          style="position: absolute; left: 20px; top: 20px; z-index: 5;"
          @click="resetSimulation"
          v-on="on">
          Reset
        </v-btn>
      </template>
      Reset the simulation
    </v-tooltip>

    <svg class='graph' :id='this.graphSvgId' style="position: absolute; z-index: 0;" ref="svg">

    </svg>
    <!--Sidebar showing all of the current event handlers' statuses-->
    <!--<v-radio-group v-model="nodeTypeToInsert" style="position: relative; top: 220px; width: 150px;">-->
    <!--<v-radio label="SYSPLACE" value="SYSPLACE"/>-->
    <!--<v-radio label="ENVPLACE" value="ENVPLACE"/>-->
    <!--<v-radio label="TRANSITION" value="TRANSITION"/>-->
    <!--</v-radio-group>-->
    <!--<v-radio-group v-model="dragDropMode" style="position: relative; top: 180px; width: 150px;">-->
    <!--<v-radio label="move nodes" value="moveNode"/>-->
    <!--<v-radio label="draw flows" value="drawFlow"/>-->
    <!--</v-radio-group>-->
    <!--<v-radio-group v-model="leftClickMode" style="position: relative; top: 140px; width: 150px;">-->
    <!--<v-radio label="unfreeze nodes" value="unfreezeNode"/>-->
    <!--<v-radio label="delete nodes" value="deleteNode"/>-->
    <!--<v-radio label="select node" value="selectNode"/>-->
    <!--</v-radio-group>-->
    <!--<v-radio-group v-model="backgroundClickMode" style="position: relative; top: 120px; width: 150px;">-->
    <!--<v-radio label="cancel selection" value="cancelSelection"/>-->
    <!--<v-radio label="insert node" value="insertNode"/>-->
    <!--</v-radio-group>-->
    <!--<v-radio-group v-model="backgroundDragDropMode" style="position: relative; top: 100px; width: 150px;">-->
    <!--<v-radio label="zoom and pan" value="zoom"/>-->
    <!--<v-radio label="select nodes" value="selectNodes"/>-->
    <!--</v-radio-group>-->
  </div>
</template>

<script>
  import * as d3 from 'd3'
  import {saveFileAs} from '../fileutilities'
  import {layoutNodes} from '../autoLayout'
  import {noOpImplementation} from '../modelCheckingRoutes'
  import {rectanglePath, arcPath, loopPath, containingBorder, pathForLink} from '../svgFunctions'
  import 'd3-context-menu/css/d3-context-menu.css'
  import contextMenuFactory from 'd3-context-menu'
  import {debounce} from 'underscore'

  const ResizeSensor = require('css-element-queries/src/ResizeSensor')
  import Vue from 'vue'
  import ToolPicker from './ToolPicker'

  import logging from '../logging'

  export default {
    name: 'graph-editor',
    components: {
      ToolPicker
    },
    props: {
      petriNetApt: {
        type: String,
        required: false // Present only for GraphEditors which display Petri Nets
      },
      restEndpoints: {
        type: Object,
        required: true
      },
      graph: {
        type: Object,
        required: true
      },
      petriNetId: {
        type: String,
        required: false
      },
      shouldShowPhysicsControls: {
        type: Boolean,
        default: false
      },
      shouldShowPartitions: {
        type: Boolean,
        default: false
      },
      editorMode: {
        type: String,
        required: true,
        validator: function (mode) {
          return ['Editor', 'Simulator', 'Viewer'].includes(mode)
        }
      },
      // This indicates whether features related to model checking should be here.
      // If this is true, then you have to supply modelCheckingRoutes as well.
      useModelChecking: {
        type: Boolean,
        default: false
      },
      modelCheckingRoutes: {
        type: Object,
        required: false,
        default: noOpImplementation
      },
      repulsionStrengthDefault: {
        type: Number,
        default: 120
      },
      linkStrengthDefault: {
        type: Number,
        default: 0.05
      },
      gravityStrengthDefault: {
        type: Number,
        default: 100
      }
    },
    data() {
      return {
        gameSimulationHistory: null,
        /* {
             currentIndex: 0, // The index of the currently selected state in the history
        Each game simulation state in this stack consists of an object:
        {
          graph: null,  // The same as our prop 'graph'
          apt: null  // The apt corresponding to the graph
          transitionFired: null // The transition fired from the previous state to reach this state
        }
        stack: []
      },*/
        dimensions: {
          width: 0,
          height: 0
        },
        winningCondition: '',
        selectedWinningCondition: '',
        ltlFormula: '', // The LTL or Flow-LTL formula corresponding to our winning condition
        ltlParseErrors: [], // If there is a server-side error parsing the formula, it gets put in here
        ltlParseStatus: 'success',
        drawTransitPreviewLinks: [],
        drawTransitPreviewSource: undefined,
        drawTransitPreviewTransition: undefined,
        drawTransitPreviewPostset: [],
        // TODO consider using a set instead of an array to prevent bugs from happening
        selectedNodes: [],
        // TODO figure out how to initialize.  Should be element of toolPickerItems
        selectedTool: undefined,
        backgroundClickMode: 'cancelSelection',
        backgroundDragDropMode: 'selectNodes',
        leftClickMode: 'selectNode',
        dragDropMode: 'moveNode',
        linkClickMode: 'doNothing',
        nodeTypeToInsert: 'SYSPLACE',
        nodeRadius: 27,
        exportedGraphJson: {},
        lastUserClick: undefined,
        physicsSimulation: d3.forceSimulation()
          .force('gravity', d3.forceManyBody().distanceMin(1000))
          .force('charge', d3.forceManyBody())
          //          .force('center', d3.forceCenter(width / 2, height / 2))
          .force('link', d3.forceLink()
            .id(link => link.id))
          .alphaMin(0.002)
          .alpha(0.7)
          .alphaTarget(0),
        repulsionStrength: this.repulsionStrengthDefault,
        linkStrength: this.linkStrengthDefault,
        gravityStrength: this.gravityStrengthDefault
      }
    },
    created: function () {
    },
    mounted: function () {
      this.nodes = []
      this.links = []
      this.importGraph(this.graph)
      this.initializeD3()
      this.updateRepulsionStrength(this.repulsionStrength)
      this.updateLinkStrength(this.linkStrength)
      this.updateGravityStrength(this.gravityStrength)
      this.updateSvgDimensions()
      this.$refs.rootElement.addEventListener('keyup', (event) => {
        logging.logObject(event)
        switch (event.key) {
          case 'Escape':
            // Allow pressing Esc to clear the temporary notification area
            if (this.selectedNodes.length === 0 &&
              this.drawTransitHandler.getCurrentState() === 0) {
              logging.resetNotification()
            }
            this.selectedNodes = []
            this.drawTransitHandler.reset()
            break
          case 'Delete':
            this.deleteSelectedNodes()
            break
          case 'Enter':
          case 'Return':
            this.drawTransitHandler.finish()
            break
        }
      })
      const parent = this.$refs.rootElement.parentElement
      this.updateGraphEditorDimensions = () => {
        const width = parent.clientWidth
        const height = parent.clientHeight
        if (width === 0 || height === 0) {
          return // Ignore spurious resizes that lead to the graph editor vanishing
          // TODO this seems to be a bug in the css-element-queries library?
          // Or we are doing something strange and unsupported, maybe
        }
        this.dimensions = {
          width: width,
          height: height
        }
        // console.log(`Graph editor dimensions: ${width}, ${height}`)
      }
      // Resize the SVG when the graph editor pane gets resized
      // eslint-disable-next-line no-new
      new ResizeSensor(parent, this.updateGraphEditorDimensions)
      // Also detect zooming in/out
      window.addEventListener('resize', this.updateGraphEditorDimensions)
      // It needs to get resized once immediately after mounting
      Vue.nextTick(this.updateGraphEditorDimensions)

      // TODO move to created() hook?
      this.selectedTool = this.toolPickerItems[0]
    },
    beforeDestroy: function () {
      console.log('beforeDestroy() hook called for GraphEditor with uid ' + this._uid)
      // When this component is destroyed, we need to stop the D3 forceSimulation from running,
      // or else it will just keep running in the background and using up cpu cycles.
      // This is an issue when, for example, the component is reloaded using Hot Reload during
      // development.
      console.log('Stopping forceSimulation')
      this.physicsSimulation.stop()
      console.log('Removing resize event listener')
      window.removeEventListener('resize', this.updateGraphEditorDimensions)
      this.isDestroyed = true
    },
    computed: {
      visibleSimulationHistory: function () {
        if (this.gameSimulationHistory) {
          return this.gameSimulationHistory
        } else {
          return {
            currentIndex: 0,
            stack: [
              {
                transitionFired: '<start>'
              }
            ]
          }
        }
      },
      toolPickerItems: function () {
        switch (this.editorMode) {
          case 'Viewer':
            return [
              this.selectToolInvisible,
              ...this.viewTools
            ]
          case 'Simulator':
            return [
              this.selectTool,
              this.fireTransitionTool,
              {type: 'divider'},
              ...this.viewTools
            ]
          case 'Editor':
            return [
              this.selectTool,
              ...this.drawingTools,
              {type: 'divider'},
              ...this.selectionTools,
              {type: 'divider'},
              ...this.viewTools
            ]
        }
      },
      selectToolInvisible: function () {
        return {
          ...this.selectTool,
          visible: false
        }
      },
      selectTool: function () {
        return {
          type: 'tool',
          icon: 'mouse',
          toolEnumName: 'select',
          name: 'Select'
        }
      },
      fireTransitionTool: function () {
        return {
          type: 'tool',
          icon: 'offline_bolt',
          toolEnumName: 'fireTransitions',
          name: 'Fire transitions'
        }
      },
      drawingTools: function () {
        return [
          {
            type: 'tool',
            icon: 'delete',
            toolEnumName: 'deleteNodesAndFlows',
            name: 'Delete'
          },
          {
            type: 'tool',
            icon: 'create',
            toolEnumName: 'drawFlow',
            name: 'Draw Flow'
          },
          {
            type: 'tool',
            icon: 'create',
            toolEnumName: 'drawTransit',
            name: 'Draw Transit'
          },
          {
            type: 'tool',
            icon: 'add',
            toolEnumName: 'insertSysPlace',
            name: this.useModelChecking ? 'Add Place' : 'Add System Place'
          },
          {
            type: 'tool',
            icon: 'add',
            visible: !this.useModelChecking,
            toolEnumName: 'insertEnvPlace',
            name: 'Add Environment Place'
          },
          {
            type: 'tool',
            icon: 'add',
            toolEnumName: 'insertTransition',
            name: 'Add Transition'
          }
        ]
      },
      selectionTools: function () {
        return [
          {
            type: 'action',
            name: 'Invert selection',
            icon: 'invert_colors',
            action: this.invertSelection
          },
          {
            type: 'action',
            name: 'Delete selected nodes',
            icon: 'delete_sweep',
            action: this.deleteSelectedNodes
          }
        ]
      },
      viewTools: function () {
        return [
          {
            type: 'action',
            name: 'Auto-Layout',
            icon: 'reorder',
            action: this.autoLayout
          },
          {
            type: 'action',
            name: 'Zoom to fit',
            icon: 'zoom_out_map',
            action: this.zoomToFitAllNodes
          },
          {
            type: 'action',
            name: 'Move all nodes to the visible area',
            icon: 'fullscreen_exit',
            action: this.moveNodesToVisibleArea
          },
          {
            type: 'action',
            name: 'Freeze all nodes',
            icon: 'leak_remove',
            action: this.freezeAllNodes
          },
          {
            type: 'action',
            name: 'Unfreeze all nodes',
            icon: 'leak_add',
            action: this.unfreezeAllNodes
          }
        ]
      },
      winningConditions: function () {
        if (this.useModelChecking) {
          return [
            {
              text: 'LTL or Flow-LTL',
              value: 'LTL'
            },
            {
              text: 'Reachability',
              value: 'A_REACHABILITY'
            },
            {
              text: 'Safety',
              value: 'A_SAFETY'
            },
            {
              text: 'Büchi',
              value: 'A_BUCHI'
            }
          ]
        } else {
          return [
            'E_REACHABILITY',
            'A_REACHABILITY',
            'E_SAFETY',
            'A_SAFETY',
            'E_BUCHI',
            'A_BUCHI',
            'E_PARITY',
            'A_PARITY']
        }
      },
      ltlParseStatusIcon: function () {
        if (this.winningCondition !== 'LTL') {
          return 'blank'
        }
        switch (this.ltlParseStatus) {
          case 'success':
            return 'thumb_up'
          case 'error':
            return 'thumb_down'
          case 'running':
            return 'hourglass_empty'
          default:
            return 'blank'
        }
      },
      closeContextMenu: function () {
        return () => {
          contextMenuFactory('close')
        }
      },
      openContextMenu: function () {
        return (d) => {
          const contextMenuFun = contextMenuFactory(this.contextMenuItems, {
            onClose: () => {
              console.log('closed context menu')
              this.highlightedDatum = null
            }
          })
          this.highlightedDatum = d
          contextMenuFun(d)
        }
      },
      // TODO Consider turning these 'computed functions' into methods.  (I think the result would
      //  be more or less the same.)
      contextMenuItems: function () {
        return (d) => {
          if (d.type === 'petriNetLink') {
            return this.contextMenuItemsFlow(d)
          } else if (this.selectedNodes.length > 1) {
            return this.contextMenuItemsSelection
          } else if (d.type === 'TRANSITION') {
            return this.contextMenuItemsNormal.concat(this.contextMenuItemsTransition(d))
          } else {
            return this.contextMenuItemsNormal.concat(this.contextMenuItemsPlace)
          }
        }
      },
      contextMenuItemsFlow: function () {
        return (clickedLink) => {
          const deleteFlow = {
            title: 'Delete Flow',
            action: (d) => {
              this.$emit('deleteFlow', {
                sourceId: d.source.id,
                targetId: d.target.id
              })
            }
          }
          const setInhibitorArc = {
            title: (d) => d.isInhibitorArc ? 'Set not inhibitor arc' : 'Set inhibitor arc',
            action: (d) => {
              this.$emit('setInhibitorArc', {
                sourceId: d.source.id,
                targetId: d.target.id,
                isInhibitorArc: !d.isInhibitorArc
              })
            }
          }
          const title = {
            title: (d) => `Flow ${d.source.id} -> ${d.target.id}`
          }
          if (this.useModelChecking && clickedLink.target.type === 'TRANSITION') {
            return [title, deleteFlow, setInhibitorArc]
          } else {
            return [title, deleteFlow]
          }
        }
      },
      contextMenuItemsPlace: function () {
        // In model checking, there is no concept of "system" or "environment" places.
        // So, in that mode, we should hide the "change to [system/environment] place" menu item.
        const itemsForSynthesis = [
          {
            title: function (d) {
              if (d.type === 'ENVPLACE') {
                return 'Change to system place'
              } else {
                return 'Change to environment place'
              }
            },
            action: this.toggleEnvironmentPlace
          }
        ]
        const itemsForBothModes = [
          {
            title: 'Set initial token',
            action: this.setInitialTokenInteractively
          },
          {
            title: 'Toggle isInitialTransit',
            action: this.toggleIsInitialTransit
          },
          {
            title: 'Toggle isSpecial',
            action: this.toggleIsSpecial,
            disabled: this.winningCondition === 'LTL'
          }
        ]

        if (this.useModelChecking) {
          return itemsForBothModes
        } else {
          return itemsForSynthesis.concat(itemsForBothModes)
        }
      },
      // TODO consider making these all methods?  And putting them next to contextMenuItemsTransition
      contextMenuItemsSelection: function () {
        return [
          {
            title: 'Selection'
          },
          {
            title: 'Delete selected',
            action: this.deleteSelectedNodes
          }
        ]
      },
      contextMenuItemsNormal: function () {
        return [
          {
            title: (d) => {
              switch (d.type) {
                case 'SYSPLACE':
                case 'ENVPLACE':
                  return `Place ${d.id}`
                case 'TRANSITION':
                  return `Transition ${d.id}`
                default:
                  throw new Error('Unhandled case in switch statement for right click context menu')
              }
            }
          },
          {
            title: 'Delete',
            action: (d) => {
              this.$emit('deleteNode', d.id)
            }
          },
          {
            title: 'Rename',
            action: this.renameNodeInteractively
          }
        ]
      },
      // TODO Figure out why Strategy BDD/Graph Strat BDD / GGBDD nodes still spawn at 0,0 ???
      nodeSpawnPoint: function () {
        if (this.lastUserClick) {
          return this.lastUserClick
        } else {
          return {
            x: this.dimensions.width / 2,
            y: this.dimensions.height / 2
          }
        }
      },
      // These DOM elements should have unique IDs so that multiple GraphEditors can coexist on one page.
      rootElementId: function () {
        return 'graph-editor-' + this._uid
      },
      graphSvgId: function () {
        return 'graph-' + this._uid
      },
      arrowheadId: function () {
        return 'arrowhead-' + this._uid
      },
      // Given a d3 selection of link elements, apply to it all of the event handlers that we want links to have.
      // Usage: selection.call(applyNodeEventHandler)
      applyLinkEventHandler: function () {
        return selection => {
          selection.on('mousedown', (d) => {
            // Use mousedown instead of click because click doesn't fire until mouseup
            if (d3.event.button === 0) { // Only respond to left clicks
              this.onLinkClick(d)
            }
          })
          selection.on('contextmenu', this.onLinkRightClick)
          selection.on('mouseover', (d) => {
            d.isHovered = true
            this.updateD3()
          })
          selection.on('mouseout', (d) => {
            d.isHovered = false
            this.updateD3()
          })
        }
      },
      onLinkClick: function () {
        return (d) => {
          d3.event.stopPropagation()
          switch (this.linkClickMode) {
            case 'deleteFlow':
              console.log(`onLinkClick: Emitting deleteFlow event: source ${d.source.id}, target ${d.target.id}`)
              this.$emit('deleteFlow', {
                sourceId: d.source.id,
                targetId: d.target.id
              })
              break
            default:
              console.log(`onLinkClick: linkClickMode === ${this.linkClickMode}; Doing nothing`)
          }
        }
      },
      onLinkRightClick: function () {
        return (d) => {
          this.openContextMenu(d)
        }
      },
      // Given a d3 selection of node elements, apply to it all of the event handlers that we want nodes to have.
      // Usage: selection.call(applyNodeEventHandler)
      applyNodeEventHandler: function () {
        return selection => {
          this.dragDrop(selection)
          selection.on('click', this.onNodeClick)
          selection.on('contextmenu', this.onNodeRightClick)
          selection.on('mouseover', d => {
            d.isHovered = true
            this.updateD3()
          })
          selection.on('mouseout', d => {
            d.isHovered = false
            this.updateD3()
          })
        }
      },
      onNodeClick: function () {
        return (d) => {
          d3.event.stopPropagation()
          // TODO Rename this from GRAPH_STRATEGY_BDD_STATE to BDD_GRAPH_STATE
          if (d.type === 'GRAPH_STRATEGY_BDD_STATE') {
            // Expand or collapse the postset of the State that has been clicked.
            // Freeze the State's position
            d.fx = d.x
            d.fy = d.y

            // Save the mouse coordinates so that the new nodes will appear where the user clicked.
            // I.e. at the location of the parent node that is being expanded.
            const mouseCoordinates = this.mousePosZoom()
            this.lastUserClick = {x: mouseCoordinates[0], y: mouseCoordinates[1]}
            // Toggle whether the postset of this State is visible
            this.$emit('toggleStatePostset', d.id)
          } else {
            this.closeContextMenu()
            this.nodeClickHandler(d)
          }
        }
      },
      onNodeRightClick: function () {
        return (d) => {
          d3.event.preventDefault() // Prevent the right click menu from appearing
          d3.event.stopPropagation()
          console.log(d)
          // TODO refactor duplicate code?
          if (d.type === 'GRAPH_STRATEGY_BDD_STATE') {
            // Freeze the State's position
            d.fx = d.x
            d.fy = d.y
            const mouseCoordinates = this.mousePosZoom()
            this.lastUserClick = {x: mouseCoordinates[0], y: mouseCoordinates[1]}
            // Toggle whether the preset of this State is visible
            this.$emit('toggleStatePreset', d.id)
          } else {
            // Cancel selection if a non-selected node is clicked
            if (!this.selectedNodes.includes(d)) {
              this.selectedNodes = []
            }
            this.openContextMenu(d)
          }
        }
      },
      nodeClickHandler: function () {
        switch (this.leftClickMode) {
          case 'unfreezeNode':
            return (d) => {
              d.fx = null
              d.fy = null
            }
          case 'deleteNode':
            return (d) => {
              this.$emit('deleteNode', d.id)
            }
          case 'selectNode':
            return (d) => {
              console.log(d3.event)
              if (d3.event.ctrlKey) {
                if (this.selectedNodes.includes(d)) {
                  this.selectedNodes = this.selectedNodes.filter(node => node !== d)
                } else {
                  this.selectedNodes.push(d)
                }
              } else {
                this.selectedNodes = [d]
              }
            }
          case 'drawTransit':
            return this.drawTransitHandler.onClick
          case 'fireTransition':
            return (d) => {
              if (d.type !== 'TRANSITION') {
                return
              } else {
                this.fireTransition(d)
              }
            }
          default:
            return () => {
              logging.sendErrorNotification(`No left click handler was found for leftClickMode === ${this.leftClickMode}`)
            }
        }
      },
      drawTransitHandler: function () {
        let state = 0
        let source
        let transition
        let postset = new Set()

        // Log our state and also display it using D3
        // TODO consider refactoring this handler into its own module and using an event/callback-based
        // interface.  It's kind of spaghettilike to be updating these global variables here that
        // are used in updateD3.  Those parts of updateD3() that pertain to this tool could probably
        // be refactored out into their own method, I reckon, into which you could pass the values
        // of drawTransitPreviewSource, drawTransitPreviewLinks, etc.  That way, they would no
        // longer need to be global variables like they are now.
        // You would use it like this:
        // const handler = drawTransitHandler()
        //   .onChange({source, transition, postset} => this.updateTransitPreview(source, transition, postset))
        // Whereby the Vue instance method updateTransitPreview would pass everything into D3.
        // -Ann
        const logCurrentState = () => {
          const src = source ? source.id : 'none'
          const trans = transition ? transition.id : 'none'
          const targetList = Array.from(postset).map(t => t.id).join(', ')
          logging.logVerbose(`DrawTransit state: \nsource: ${src}\ntransition: ${trans}\npostset: ${targetList}`)
          const sourceTransLink = source !== undefined && transition !== undefined ? [{
            source: source,
            target: transition
          }] : []
          this.drawTransitPreviewLinks = sourceTransLink.concat(Array.from(postset).map(postNode => ({
            source: transition,
            target: postNode
          })))
          this.drawTransitPreviewSource = source
          this.drawTransitPreviewTransition = transition
          this.drawTransitPreviewPostset = Array.from(postset)
          this.updateD3()
        }

        const reset = () => {
          logging.logVerbose('Resetting drawTransit')
          state = 0
          source = undefined
          transition = undefined
          postset = new Set()
          this.drawTransitPreviewLinks = []
          this.drawTransitPreviewSource = undefined
          this.drawTransitPreviewTransition = undefined
          this.drawTransitPreviewPostset = []
          this.updateD3()
        }

        const getCurrentState = () => state

        return {
          getCurrentState,
          reset,
          finish: () => {
            const sourceId = source ? source.id : undefined
            if (transition && postset.size > 0) {
              this.$emit('createTransit', {
                source: sourceId,
                transition: transition.id,
                postset: Array.from(postset).map(d => d.id)
              })
            } else {
              logging.logVerbose('Aborting drawTransit.  A transition and at least one target must be specified.')
            }
            reset()
          },
          onClick: (d) => {
            switch (state) {
              case 0: {
                if (d.type === 'ENVPLACE' || d.type === 'SYSPLACE') {
                  logging.logVerbose('DrawTransit: Creating non-initial transit.')
                  source = d
                  state = 1
                } else if (d.type === 'TRANSITION') {
                  logging.logVerbose('DrawTransit: Creating initial transit.')
                  transition = d
                  state = 2
                }
                logCurrentState()
                break
              }
              case 1: {
                if (d.type === 'TRANSITION') {
                  transition = d
                  state = 2
                  logCurrentState()
                } else {
                  logging.logVerbose('DrawTransit: Please click on a transition.')
                }
                break
              }
              case 2: {
                if (d.type === 'ENVPLACE' || d.type === 'SYSPLACE') {
                  if (postset.has(d)) {
                    postset.delete(d)
                  } else {
                    postset.add(d)
                  }
                  logCurrentState()
                } else {
                  logging.logVerbose('DrawTransit: Click on Places to specify a postset and press Enter ' +
                    'to create the transit.  Press Esc to abort.')
                }
              }
            }
          }
        }
      },
      backgroundClickHandler: function () {
        switch (this.backgroundClickMode) {
          case 'insertNode':
            return () => {
              const mousePos = this.mousePosZoom()
              const nodeSpec = {
                x: mousePos[0],
                y: mousePos[1],
                type: this.nodeTypeToInsert
              }
              console.log('emitting insertNode')
              this.$emit('insertNode', nodeSpec)
              this.selectedNodes = []
            }
          case 'cancelSelection':
            return () => {
              this.selectedNodes = []
            }
          default:
            return () => {
              logging.sendErrorNotification(`No background click handler found for backgroundClickMode === ${this.backgroundClickMode}`)
            }
        }
      },
      dragDrop: function () {
        const dragDropHandlers = {
          'moveNode': this.moveNodeDragDrop,
          'drawFlow': this.drawFlowDragDrop
        }
        let dragDropHandler
        return d3.drag()
          .clickDistance(2)
          .on('start', node => {
            this.closeContextMenu()
            dragDropHandler = dragDropHandlers[this.dragDropMode]
            dragDropHandler['start'](node)
          })
          .on('drag', node => {
            dragDropHandler['drag'](node)
          })
          .on('end', node => {
            dragDropHandler['end'](node)
          })
      },
      moveNodeDragDrop: function () {
        let isSelectionDrag
        let nodeStartPositions = {} // Map from node ID -> {x, y}
        let dragStartX, dragStartY
        let snapToGrid
        return {
          'start': node => {
            isSelectionDrag = this.selectedNodes.includes(node)
            // Save the place where we started and the start position of each node.
            // We will use this later to implement a multi-drag-drop that works in all browsers.
            dragStartX = d3.event.x
            dragStartY = d3.event.y
            nodeStartPositions = {}
            this.selectedNodes.forEach(selectedNode => {
              nodeStartPositions[selectedNode.id] = {x: selectedNode.x, y: selectedNode.y}
            })

            // Freeze the node that is being dragged.
            node.fx = node.x
            node.fy = node.y
          },
          'drag': node => {
            snapToGrid = d3.event.sourceEvent.ctrlKey
            // When the user drags a node, the physics simulation should start again in case it had
            // been paused for inactivity.
            this.physicsSimulation.alpha(0.7).restart()
            if (isSelectionDrag) {
              // To you, dear reader, this might seem overcomplicated.  You might ask, "Ann,
              // why didn't you simply write this:
              // node.fx += d3.event.dx
              // node.fy += d3.event.dy?
              // The answer is that, in Firefox on Debian, the mouse events don't work exactly like
              // you might expect them to.
              // The sum of all d3.event.dx and dy does not add up exactly to the total distance moved.
              // That is why we have to calculate the distance ourselves and add it to the start
              // position of each node in the selection by hand.
              const dx = d3.event.x - dragStartX
              const dy = d3.event.y - dragStartY
              this.selectedNodes.forEach(node => {
                if (snapToGrid) {
                  const newX = snap(nodeStartPositions[node.id].x) + dx
                  const newY = snap(nodeStartPositions[node.id].y) + dy
                  const xSnapped = snap(newX)
                  const ySnapped = snap(newY)
                  node.fx = xSnapped
                  node.fy = ySnapped
                } else {
                  node.fx = nodeStartPositions[node.id].x + dx
                  node.fy = nodeStartPositions[node.id].y + dy
                }
              })
            } else {
              if (snapToGrid) {
                node.fx = snap(d3.event.x)
                node.fy = snap(d3.event.y)
              } else {
                node.fx = d3.event.x
                node.fy = d3.event.y
              }
            }
          },
          'end': node => {
            this.onDragDropEnd()
          }
        }

        function snap(x) {
          return roundToMiddle(60, x)
        }

        function roundToMiddle(roundingNumber, x) {
          return x + (roundingNumber / 2 - (x % roundingNumber))
        }
      },
      drawFlowDragDrop: function () {
        let startNode
        return {
          'start': node => {
            startNode = node
            const mousePos = this.mousePosZoom()
            this.dragLine.attr('d', `M${startNode.x},${startNode.y}L${mousePos[0]},${mousePos[1]}`)
          },
          'drag': node => {
            const mousePos = this.mousePosZoom()
            this.dragLine.attr('d', `M${startNode.x},${startNode.y}L${mousePos[0]},${mousePos[1]}`)
            this.drawFlowTarget = findFlowTarget(mousePos, startNode, this.nodes, this.links)
          },
          'end': node => {
            // figure out which node the drag ends on top of
            const nearestNode = findFlowTarget(this.mousePosZoom(), startNode, this.nodes, this.links)
            this.dragLine.attr('d', '')
            if (nearestNode === undefined) {
              console.log('No candidate node found.  Not creating a flow.')
            } else {
              console.log(`We will try to draw a flow to this node:`)
              console.log(nearestNode)
              this.$emit('createFlow', {
                source: startNode.id,
                destination: nearestNode.id
              })
            }

            this.drawFlowTarget = undefined
          }
        }

        // Your mouse cursor is at mouseX, mouseY.  You want to draw a flow that starts at startNode
        // and ends at another node which is close to the mouse cursor.  To figure out what eligible
        // end node is closest to the mouse, use this function.
        function findFlowTarget(mousePos, startNode, nodes, links) {
          let nearestNode
          // Only nodes within this many units of the startNode will be under consideration.
          let minDistance = 50
          nodes.filter(isEligible)
            .forEach(n => {
              const dx = mousePos[0] - n.x
              const dy = mousePos[1] - n.y
              const distance = Math.sqrt(dx * dx + dy * dy)
              if (distance < minDistance) {
                minDistance = distance
                nearestNode = n
              }
            })
          return nearestNode

          // Only create flows from Transition to Place or from Place to Transition
          function isEligible(node) {
            const transitionToPlace = startNode.type === 'TRANSITION' &&
              ['SYSPLACE', 'ENVPLACE'].includes(node.type)
            const placeToTransition =
              ['SYSPLACE', 'ENVPLACE'].includes(startNode.type) &&
              node.type === 'TRANSITION'
            const linkExists = links.find(link => {
              return link.source === startNode && link.target === node
            })
            return (transitionToPlace || placeToTransition) && !linkExists
          }
        }
      },
      backgroundDragDrop: function () {
        let startX, startY
        let previousSelection // These nodes were selected as this drag drop began
        return d3.drag()
          .clickDistance(2)
          .on('start', () => {
            this.closeContextMenu();
            [startX, startY] = this.mousePosZoom()
            previousSelection = this.selectedNodes.slice() // Clone the current selection
          })
          .on('drag', () => {
            const [currentX, currentY] = this.mousePosZoom()
            this.selectNodesPreview
              .attr('d', rectanglePath(startX, startY, currentX, currentY))
            const nodesInRectangle = findSelectedNodes(this.nodes, startX, startY, currentX, currentY)
            const event = d3.event.sourceEvent // d3.event is a drag event; its sourceEvent is a mouseMove
            if (event.ctrlKey) {
              // Emulate Windows Explorer's well-known ctrl + drag-select behavior
              // TODO Consider instead using ctrl to add to a selection and alt to remove from it, a la photoshop
              // TODO Update selection when ctrl is pressed or released, even if no more drag events arrive
              const newNodes = nodesInRectangle.filter(n => !previousSelection.includes(n))
              const oldNodes = previousSelection.filter(n => !nodesInRectangle.includes(n))
              const xor = newNodes.concat(oldNodes)
              this.selectedNodes = xor
            } else {
              this.selectedNodes = nodesInRectangle
            }
          })
          .on('end', () => {
            const [currentX, currentY] = this.mousePosZoom()
            console.log(`did a drag drop on the background from ${startX},${startY} to ${currentX}, ${currentY}`)
            console.log('selected nodes:')
            console.log(this.selectedNodes)
            this.selectNodesPreview.attr('d', '')
          })

        function findSelectedNodes(nodes, startX, startY, currentX, currentY) {
          return nodes.filter(node => {
            const xFits = (node.x > startX && node.x < currentX) ||
              (node.x < startX && node.x > currentX)
            const yFits = (node.y > startY && node.y < currentY) ||
              (node.y < startY && node.y > currentY)
            return xFits && yFits
          })
        }
      }
    },
    watch: {
      ltlFormula: function (formula) {
        if (this.selectedWinningCondition === 'LTL' && formula !== '') {
          this.parseLtlFormula()
        }
      },
      selectedWinningCondition: function (condition) {
        if (condition !== this.winningCondition) {
          this.$emit('setWinningCondition', condition)
        }
      },
      winningCondition: function (condition) {
        this.selectedWinningCondition = condition
        this.ltlParseStatus = ''
        this.ltlParseErrors = []
      },
      editorMode: function (newMode, oldMode) {
        if (newMode === 'Simulator') {
          this.selectedTool = this.fireTransitionTool
        } else if (this.selectedTool == this.fireTransitionTool && newMode !== 'Simulator') {
          // Prevent fireTransitionTool from being selected in Editor
          this.selectedTool = this.selectTool
        }

        if (newMode === 'Editor' && oldMode === 'Simulator') {
          this.importGraph(this.graph)
          this.updateD3()
        } else if (newMode === 'Simulator' && oldMode === 'Editor') {
          this.gameSimulationHistory = null
        }
      },
      selectedTool: function (tool) {
        // TODO Instead of watching selectedTool, create a computed property 'eventHandlers'.
        // That would be more declarative and Vue-like, I think.  -Ann
        if (tool.toolEnumName !== 'drawTransit') {
          this.drawTransitHandler.reset()
        }
        switch (tool.toolEnumName) {
          case 'select': {
            this.backgroundClickMode = 'cancelSelection'
            this.backgroundDragDropMode = 'selectNodes'
            this.leftClickMode = 'selectNode'
            this.dragDropMode = 'moveNode'
            // TODO make it possible to select a set of links
            this.linkClickMode = 'doNothing'
            break
          }
          case 'drawFlow': {
            this.backgroundDragDropMode = 'zoom'
            this.backgroundClickMode = 'cancelSelection'
            this.leftClickMode = 'selectNode'
            this.dragDropMode = 'drawFlow'
            this.linkClickMode = 'doNothing'
            break
          }
          case 'drawTransit': {
            this.backgroundDragDropMode = 'zoom'
            this.backgroundClickMode = 'cancelSelection'
            this.leftClickMode = 'drawTransit'
            this.dragDropMode = 'moveNode'
            this.linkClickMode = 'doNothing'
            break
          }
          case 'insertSysPlace': {
            this.backgroundDragDropMode = 'selectNodes'
            this.backgroundClickMode = 'insertNode'
            this.leftClickMode = 'selectNode'
            this.dragDropMode = 'moveNode'
            this.nodeTypeToInsert = 'SYSPLACE'
            this.linkClickMode = 'doNothing'
            break
          }
          case 'insertEnvPlace': {
            this.backgroundDragDropMode = 'selectNodes'
            this.backgroundClickMode = 'insertNode'
            this.leftClickMode = 'selectNode'
            this.dragDropMode = 'moveNode'
            this.nodeTypeToInsert = 'ENVPLACE'
            this.linkClickMode = 'doNothing'
            break
          }
          case 'insertTransition': {
            this.backgroundDragDropMode = 'selectNodes'
            this.backgroundClickMode = 'insertNode'
            this.leftClickMode = 'selectNode'
            this.dragDropMode = 'moveNode'
            this.nodeTypeToInsert = 'TRANSITION'
            this.linkClickMode = 'doNothing'
            break
          }
          case 'deleteNodesAndFlows': {
            this.backgroundDragDropMode = 'selectNodes'
            this.backgroundClickMode = 'cancelSelection'
            this.leftClickMode = 'deleteNode'
            this.dragDropMode = 'moveNode'
            this.linkClickMode = 'deleteFlow'
            break
          }
          case 'fireTransitions': {
            this.backgroundDragDropMode = 'selectNodes'
            this.backgroundClickMode = 'cancelSelection'
            this.leftClickMode = 'fireTransition'
            this.dragDropMode = 'moveNode'
            this.linkClickMode = 'doNothing'
            break
          }
          default: {
            logging.sendErrorNotification('Unknown tool: ' + tool)
          }
        }
      },
      selectedNodes: function () {
        this.updateD3()
      },
      repulsionStrength: function (strength) {
        this.updateRepulsionStrength(strength)
      },
      linkStrength: function (strength) {
        this.updateLinkStrength(strength)
      },
      gravityStrength: function (strength) {
        this.updateGravityStrength(strength)
      },
      shouldShowPartitions: function () {
        this.updateD3()
      },
      useModelChecking: function () {
        this.updateD3()
      },
      graph: function (graph) {
        console.log('GraphEditor: graph changed:')
        console.log(graph)
        /* When graph changes, this most likely means that the user changed something in the
         APT editor, causing the APT to be parsed on the server, yielding a new graph.
         And then they hit the button "Send Graph to Editor".

         This would also be fired if the 'graph' prop changed in response to any other
         events, such as after "Load"ing a saved graph in the main App's UI.

         In response, we will update the graph that is being edited in the drag-and-drop GUI of
         this component.
         */
        this.importGraph(graph)

        this.updateD3()
      },
      dimensions: function () {
        this.updateSvgDimensions()
      }
    },
    methods: {
      // When a transition gets fired (whether successful or not), it and its connected Places
      // should flash red or green.
      showTransitionFired: function ({transitionId, wasSuccessful}) {
        const transitionD = this.nodes.find(d => d.id === transitionId)
        const matchingTransitionEl = this.nodeElements.filter(nodeD => {
          const isTheTransition = nodeD == transitionD
          const isAnAffectedPlace = this.links.some(link =>
            (link.source == transitionD && link.target == nodeD) ||
            (link.target == transitionD && link.source == nodeD)
          )
          return isTheTransition || isAnAffectedPlace
        })

        // Instantly set the color to either red or green
        matchingTransitionEl.attr('fill',
          wasSuccessful ? '#00ff00' : '#ff0000')

        // Mark the nodes as being mid-animation so they won't get messed up in updateD3()
        matchingTransitionEl.each(d => d.hasScheduledAnimation = true)

        // Gradually fade back to the normal color of the nodes
        // Note to future maintainers: "transition()" refers here to the D3 concept of transitions,
        // which are used for gradual animations like this.
        // Unfortunately, this is a bit of a namespace conflict for us.  :D
        matchingTransitionEl.transition()
          .attr('fill', this.fillOfNodeElement)
          .duration(1000)
          .ease(d3.easeLinear)
          // Then, in case the node's proper color has changed since the transition began, update it again
          .transition()
          .duration(0)
          .attr('fill', this.fillOfNodeElement)
          .on('end',
            // Mark the node as no longer being mid-animation, so updateD3() may affect it again
            () => matchingTransitionEl.each(d => d.hasScheduledAnimation = false))
      },
      resetSimulation: function () {
        this.importGraph(this.graph)
        this.gameSimulationHistory = null
        this.updateD3()
        logging.sendSuccessNotification('Reset the simulation.')
      },
      parseLtlFormula: debounce(async function () {
        console.log('Parsing Formula')
        // TODO Show 'running' status somehow in gui to distinguish it from 'success'
        // TODO Implement a timeout in case the server takes a really long time to respond
        this.ltlParseStatus = 'running'
        this.ltlParseErrors = []
        try {
          const result = await this.modelCheckingRoutes.parseLtlFormula(this.petriNetId, this.ltlFormula)
          console.log(result)
          switch (result.data.status) {
            case 'success': {
              this.ltlParseStatus = 'success'
              this.ltlParseErrors = []
              break
            }
            case 'error': {
              this.ltlParseStatus = 'error'
              this.ltlParseErrors = [result.data.message]
              break
            }
            default:
              throw new Error('Unknown status from server: ' + result.data.status)
          }
        } catch (error) {
          logging.logError('Error parsing formula: ' + error)
          this.ltlParseStatus = 'error'
          this.ltlParseErrors = [error]
        }
      }, 200),
      fireTransition: function (d) {
        if (!this.gameSimulationHistory) {
          this.gameSimulationHistory = {
            currentIndex: 0,
            stack: [
              {
                graph: this.deepCopy(this.graph),
                apt: this.petriNetApt,
                transitionFired: '<start>'
              }
            ]
          }
        }
        const {stack, currentIndex} = this.gameSimulationHistory
        const currentState = stack[currentIndex]
        if (!currentState.apt) {
          // Nets in the editor do not have their APT always stored clientside, because sometimes,
          // generating APT results in an unnecessary RenderException.  Instead, they are kept as
          // persistent objects on the server addressed by UUIDs (petriNetId).
          // So, when you switch from Editor to Simulator and want to fire a transition for the
          // first time, the client will send this petriNetId instead of an APT string in order to
          // bootstrap the simulation.
          // After that, the relevant state of the simulation (pairs of APT & graph) is kept
          // entirely client-side.
          // TODO If the RenderException issue can be resolved, I would plan to store the editor
          //  state as APT on the client all the time to reduce this complexity and make undo/redo
          //  easy to implement.
          const isUsingNetFromEditor = currentIndex === 0 && this.petriNetId
          if (!isUsingNetFromEditor) {
            logging.sendErrorNotification('No APT nor petriNetId available.  Can\'t simulate')
            return
          }
        }
        const transitionId = d.id
        this.restEndpoints.fireTransitionPure({
          // The server will simulate using the net represented in currentState.apt if present,
          // or it will fall back to the net stored on the server addressed by petriNetId.
          apt: currentState.apt,
          petriNetId: this.petriNetId,
          transitionId
        }).then(response => {
          if (response.data.status === 'success') {
            const newState = {
              ...response.data.result,  // apt, graph
              transitionFired: transitionId
            }
            this.gameSimulationHistory.stack = stack.slice(0, currentIndex + 1).concat([newState])
            this.gameSimulationHistory.currentIndex = currentIndex + 1
            this.importGraph(newState.graph)
            this.updateD3()
            this.showTransitionFired({
              transitionId: d.id,
              wasSuccessful: true
            })
            logging.sendSuccessNotification('Fired transition ' + d.id)
          } else if (response.data.status === 'error') {
            this.showTransitionFired({
              transitionId: d.id,
              wasSuccessful: false
            })
            logging.sendErrorNotification(response.data.message)
          } else {
            logging.sendErrorNotification('Invalid response from server')
          }
        })
      },
      toggleEnvironmentPlace: function (d) {
        this.$emit('toggleEnvironmentPlace', d.id)
      },
      toggleIsInitialTransit: function (d) {
        this.$emit('toggleIsInitialTransit', d.id)
      },
      toggleIsSpecial: function (d) {
        this.$emit('setIsSpecial', {
          nodeId: d.id,
          newSpecialValue: !d.isSpecial
        })
      },
      renameNodeInteractively: function (d) {
        const callback = (text) => {
          console.log('Emitting renameNode')
          // Include the x/y coordinates so they can be re-applied to the node later
          this.$emit('renameNode', {
            idOld: d.id,
            idNew: text,
            xOld: d.x,
            yOld: d.y
          })
        }
        console.log('Opening text input box to rename the following node:')
        console.log(d)
        this.getTextInput(`Rename ${d.id}`, callback)
      },
      setInitialTokenInteractively: function (d) {
        const callback = (text) => {
          const tokens = parseInt(text)
          if (isNaN(tokens)) {
            throw new Error('The text entered can\'t be parsed into an integer, so we can\'t set' +
              ' the initial token count to that.')
          } else {
            console.log('Emitting setInitialToken')
            this.$emit('setInitialToken', {
              nodeId: d.id,
              tokens: tokens
            })
          }
        }
        this.getTextInput(`Set initial token for ${d.id}`, callback)
      },
      // Open a text input field in the svg and focus it.  Let the user type stuff in, and call
      // callback with whatever text the user entered.
      // This is meant to be called in a mouse click handler so that the text input box appears by
      // the mouse cursor.
      // TODO Make this look better
      getTextInput: function (label, callback) {
        const [mouseX, mouseY] = this.mousePosZoom()
        const fo = this.container.append('foreignObject')
          .attr('x', mouseX)
          .attr('y', mouseY - 40)
          .attr('width', '200px')
          .attr('height', '100px')
        const body = fo.append('xhtml:body')
        body.append('span').text(label)
        const textInput = body.append('form')
          .append('input')
          .attr('type', 'text')
          .attr('background', '#BBBBEE')
          .on('keyup', () => {
            if (d3.event.key === 'Enter') {
              const text = textInput.node().value
              fo.remove()
              console.log(`Calling text entry callback with value '${text}'`)
              callback(text)
            } else if (d3.event.key === 'Escape') {
              fo.remove()
            }
          })
          .on('blur', () => {
            fo.remove()
          })
        textInput.node().focus()
      },
      invertSelection: function () {
        this.selectedNodes = this.nodes.filter(node => !this.selectedNodes.includes(node))
      },
      deleteSelectedNodes: function () {
        console.log('deleting selected nodes')
        // TODO There's a bug here. If we send a bunch of requests in a row, the responses may come
        // out of order, leaving us // in an inconsistent state with the server.
        // A possible solution: Just send a single request with a set of node IDs to be deleted.
        this.selectedNodes.forEach(node => {
          this.$emit('deleteNode', node.id)
        })
        this.selectedNodes = []
      },
      // Return zoom-transformed x/y coordinates of mouse cursor as a 2-element array [x, y]
      mousePosZoom: function () {
        const mousePos = d3.mouse(this.svg.node())
        const transform = d3.zoomTransform(this.svg.node())
        return transform.invert(mousePos)
      },
      onLoadNewPetriGame: function () {
        // When we load a new petri game, the positions of nodes from the previously loaded Petri Game
        // should not be carried over.
        this.randomizeAllNodesPositions()
      },
      randomizeAllNodesPositions: function () {
        this.nodes.forEach(node => {
          node.fx = undefined
          node.fy = undefined
          node.x = this.dimensions.width / 2 + (Math.random() - 0.5) * 40
          node.y = this.dimensions.height / 2 + (Math.random() - 0.5) * 40
        })
      },
      updateSvgDimensions: function () {
        this.svg.attr('width', `${this.dimensions.width}px`)
        this.svg.attr('height', `${this.dimensions.height}px`)
        this.updateCenterForce()
      },
      /**
       * We try to keep the Petri Net centered in the middle of the viewing area by applying a force to it.
       */
      updateCenterForce: function () {
        const transform = d3.zoomTransform(this.svg.node())
        const centerX = transform.invertX(this.dimensions.width / 2)
        const centerY = transform.invertY(this.dimensions.height / 2)
        // console.log(`Updating center force to coordinates: ${centerX}, ${centerY}`)
        // forceCenter is an alternative to forceX/forceY.  It works in a different way.  See D3's documentation.
        // this.physicsSimulation.force('center', d3.forceCenter(svgX / 2, svgY / 2))
        const centerStrength = 0.01
        this.physicsSimulation.force('centerX', d3.forceX(centerX).strength(centerStrength))
        this.physicsSimulation.force('centerY', d3.forceY(centerY).strength(centerStrength))
      },
      // TODO Run this whenever a new graph is loaded.  (But not upon changes to an existing graph)
      autoLayout: function () {
        const boundingRect = this.svg.node().getBoundingClientRect()
        // There is a transformation applied to the SVG container using d3-zoom.
        // Calculate the actual visible area's margins using the inverse of the transform.
        const transform = d3.zoomTransform(this.svg.node())
        const minX = transform.invertX(0)
        const maxX = transform.invertX(boundingRect.width)
        const minY = transform.invertY(boundingRect.top)
        const maxY = transform.invertY(boundingRect.bottom)
        const positionsPromise = layoutNodes(this.nodes, this.links, 0.15, minX, maxX, minY, maxY)
        positionsPromise.then(positions => {
          this.freezeAllNodes()
          this.nodes.forEach(node => {
            const position = positions[node.id]
            if (node.fx === node.x) {
              node.fx = position.x
              node.fy = position.y
            } else {
              node.x = position.x
              node.y = position.y
            }
          })
          this.updateD3()
        })
      },
      saveGraph: function () {
        const html = this.svg.node().outerHTML
        saveFileAs(html, 'graph.svg')
        console.log(html)
      },
      // Stop all the nodes from moving.
      freezeAllNodes: function () {
        this.nodes.forEach(node => {
          node.fx = node.x
          node.fy = node.y
        })
      },
      unfreezeAllNodes: function () {
        if (confirm('Are you sure you want to unfreeze all nodes?  ' +
          'The fixed positions you have moved them to will be lost.')) {
          this.nodes.forEach(node => {
            node.fx = null
            node.fy = null
          })
        }
        this.physicsSimulation.alpha(0.7).restart()
      },
      // If you lost track of where the graph actually is, you can click this button and it will
      // zoom out far enough that all the nodes can be seen.  :)
      zoomToFitAllNodes: function () {
        const bbox = this.container.node().getBBox()
        const svgWidth = this.svgWidth()
        const svgHeight = this.svgHeight()
        const containerCenter = [
          bbox.x + bbox.width / 2,
          bbox.y + bbox.height / 2]
        const scale = 0.8 / Math.max(bbox.width / svgWidth, bbox.height / svgHeight)
        const translate = [
          // Add 40 to account for the vertical toolbar on the left side of the screen
          // and the 'ltl formula' / 'winning condition' input
          svgWidth / 2 - scale * containerCenter[0] + 40,
          svgHeight / 2 - scale * containerCenter[1] + 40
        ]
        this.svg.transition().duration(300).call(this.zoom.transform, d3.zoomIdentity.translate(translate[0], translate[1]).scale(scale))
      },
      // Sometimes nodes might get lost outside the borders of the screen.
      // This procedure places them back within the visible area.
      moveNodesToVisibleArea: function () {
        const margin = 70
        const boundingRect = this.svg.node().getBoundingClientRect()
        // There is a transformation applied to the SVG container using d3-zoom.
        // Calculate the actual visible area's margins using the inverse of the transform.
        const transform = d3.zoomTransform(this.svg.node())

        // Add 40 to account for the vertical toolbar on the left side of the screen
        const minX = transform.invertX(margin + 40)
        const maxX = transform.invertX(boundingRect.width - margin)
        const minY = transform.invertY(margin)
        const maxY = transform.invertY(boundingRect.height - margin)
        this.nodes.forEach(node => {
          let nodeHasBeenMoved = false
          if (node.x < minX) {
            nodeHasBeenMoved = true
            node.fx = minX
          }
          if (node.y < minY) {
            nodeHasBeenMoved = true
            node.fy = minY
          }
          if (node.x > maxX) {
            nodeHasBeenMoved = true
            node.fx = maxX
          }
          if (node.y > maxY) {
            nodeHasBeenMoved = true
            node.fy = maxY
          }
          if (nodeHasBeenMoved) {
            // Make sure the node is frozen on both the x and y axes.  Otherwise it feels weird
            if (!node.fy) {
              node.fy = node.y
            }
            if (!node.fx) {
              node.fx = node.x
            }
          }
        })
        this.updateD3()
      },
      getNodeXYCoordinates: function () {
        // Convert our array of nodes to a map with node IDs as keys and x,y coordinates as value.
        return this.nodes.reduce(function (map, node) {
          map[node.id] = {
            x: node.x.toFixed(2),
            y: node.y.toFixed(2)
          }
          return map
        }, {})
      },
      onDragDropEnd: function () {
        this.$emit('dragDropEnd')
      },
      initializeD3: function () {
        this.svg = d3.select('#' + this.graphSvgId)

        // Add SVG namespace so that SVG can be exported
        this.svg.attr('xmlns:xlink', 'http://www.w3.org/1999/xlink')
        this.svg.attr('xmlns', 'http://www.w3.org/2000/svg')

        // Define arrows
        this.svg.append('svg:defs').selectAll('marker')
          .data(['end'])      // Different link/path types can be defined here
          .enter().append('svg:marker')    // This section adds in the arrows
          .attr('id', this.arrowheadId)
          .attr('viewBox', '0 -5 10 10')
          .attr('refX', 10) // This is 10 because the arrow is 10 units long
          .attr('refY', 0)
          .attr('markerWidth', 6)
          .attr('markerHeight', 6)
          .attr('orient', 'auto')
          .append('svg:path')
          .attr('d', 'M0,-5L10,0L0,5')
        const onZoom = () => {
          const transform = d3.zoomTransform(this.svg.node())
          this.container.attr('transform', `translate(${transform.x}, ${transform.y}) scale(${transform.k})`)
          this.updateCenterForce()
        }
        this.zoom = d3.zoom()
          .on('zoom', onZoom)
          .wheelDelta(() => {
            return -d3.event.deltaY * (d3.event.deltaMode ? 120 : 1) / 1500
          })
          .filter(() => {
            const isWheel = d3.event instanceof WheelEvent
            if (isWheel) {
              return true
            } else {
              const hotkeyHeldDown = d3.event.shiftKey
              const isLeftClick = d3.event.button === 0
              const isZoomMode = this.backgroundDragDropMode === 'zoom'
              return isLeftClick && (hotkeyHeldDown || isZoomMode)
            }
          })
        this.svg.call(this.zoom)
        this.svg.on('mousedown', d => {
          // Use mousedown instead of click because click doesn't fire until mouseup
          if (d3.event.button !== 0) {
            return // Only respond to left clicks
          }
          this.closeContextMenu()
          this.backgroundClickHandler(d)
        })
        this.backgroundDragDrop(this.svg)

        this.container = this.svg.append('g')
        this.linkGroup = this.container.append('g').attr('class', 'links')
        this.linkTextGroup = this.container.append('g').attr('class', 'linkTexts')
        this.nodeGroup = this.container.append('g').attr('class', 'nodes')
        this.isSpecialGroup = this.container.append('g').attr('class', 'isSpecialHighlights')
        this.drawTransitPreviewGroup = this.container.append('g').attr('class', 'drawTransitPreview')
        this.inhibitorArcCircleGroup = this.container.append('g')
          .attr('class', 'inhibitorArcCircle')
        this.labelGroup = this.container.append('g').attr('class', 'texts')
        this.contentGroup = this.container.append('g').attr('class', 'node-content')
        // This is the arrow that we draw when the user is adding a transition between two nodes
        // (via click-and-drag)
        this.dragLine = this.container.append('path')
          .attr('stroke-width', 3)
          .attr('fill', 'none')
          .attr('stroke', '#000000')
          .attr('marker-end', 'url(#' + this.arrowheadId + ')')
          .attr('d', '')
        // This is the "preview circle" that highlights the node that a flow will be drawn to
        // when the user is doing a drag-drop to draw a flow
        this.drawFlowPreview = this.container.append('circle')
          .attr('stroke', 'black')
          .attr('stroke-width', 2)
          .attr('fill-opacity', 0)
        // This is the rectangle shown when the user is trying to select a group of nodes
        this.selectNodesPreview = this.container.append('path')
          .attr('stroke', 'black')
          .attr('fill', 'none')
          .attr('stroke-width', 2)
        // This is the border drawn around all selected nodes.
        this.selectionBorder = this.container.append('path')
          .attr('stroke', '#000099')
          .attr('fill', 'none')
          .attr('stroke-width', 0) // TODO Only draw this border when we need it for 'stretching'

        this.updateD3()

//        d3.selectAll('*').on('click', function (d) { console.log(d) })
      },
      /**
       * This method should be called every time the "nodes" or "links" arrays are updated.
       * It causes our visualization to update accordingly, showing new nodes and removing deleted ones.
       */
      updateD3: function () {
        console.log('updateD3()')
        // Write the IDs/labels of nodes underneath them.
        // TODO Prevent these from getting covered up by arrowheads.  Maybe add a background.
        // See https://stackoverflow.com/questions/15500894/background-color-of-text-in-svg
        const newLabelElements = this.labelGroup
          .selectAll('text')
          .data(this.nodes, this.keyFunction)
        const labelEnter = newLabelElements
          .enter().append('text')
          .call(this.applyNodeEventHandler)
          .attr('text-anchor', 'middle')
        newLabelElements.exit().remove()
        this.labelElements = labelEnter.merge(newLabelElements)
        this.labelElements
          .attr('font-size', 15)
          .attr('font-weight', d => {
            return 'normal'
          })
          .text(node => {
            // This code is specifically for Graph Strategy BDDs where the whole BDD is too big to
            // show at once, so we hide part of the graph at first and explore it by clicking on it
            const invisibleChildrenMarker = node.hasInvisibleChildren ? '*' : ''
            const invisibleParentsMarker = node.hasInvisibleParents ? '*' : ''
            return `${invisibleParentsMarker}${node.label}${invisibleChildrenMarker}`
          })

        // Write text inside of nodes.  (Petri Nets have token numbers.  BDDGraphs have "content")
        const newContentElements = this.contentGroup
          .selectAll('text')
          .data(this.nodes.filter(node => node.content !== undefined || node.initialToken !== undefined), this.keyFunction)
        const contentEnter = newContentElements
          .enter().append('text')
          .call(this.applyNodeEventHandler)
          .attr('text-anchor', 'middle')
          .attr('dy', '-8')
          .attr('font-family', '\'Inconsolata\', monospace')
          // TODO Bug: The white-space attribute is not implemented for SVGs in Google Chrome.
          // TODO This means that our text will end up all on one line.  In Firefox it's ok, though.
          .style('white-space', 'pre')
        newContentElements.exit().remove()
        this.contentElements = contentEnter.merge(newContentElements)
        this.contentElements
          .attr('font-size', node => {
            if (node.type === 'TRANSITION') {
              return 28
            } else if (node.type === 'ENVPLACE' || node.type === 'SYSPLACE') {
              return 20
            } else if (node.type === 'GRAPH_STRATEGY_BDD_STATE') {
              return 15
            }
          })
          .text(node => {
            if (node.type === 'GRAPH_STRATEGY_BDD_STATE') {
              // Figure out how long the widest line of the content is to determine node width later
              const lines = node.content.split('\n')
              const numberOfLines = lines.length
              node.numberOfLines = numberOfLines
              const lengthsOfLines = lines.map(str => str.length)
              const maxLineLength = lengthsOfLines.reduce((max, val) => val > max ? val : max, 0)
              node.maxContentLineLength = maxLineLength
              // logging.logVerbose(`max content line length: ${maxLineLength}`)
              return node.content
            } else if (node.type === 'ENVPLACE' || node.type === 'SYSPLACE') {
              return node.initialToken === 0 ? '' : node.initialToken
            } else if (node.type === 'TRANSITION' && node.isReadyToFire) {
              return '*'
            }
          })

        // Draw circles around Places in Petri Nets with isSpecial = true
        const isSpecialElements = this.isSpecialGroup
          .selectAll('circle')
          .data(this.nodes.filter(node => node.isSpecial === true), this.keyFunction)
        const newIsSpecialElements = isSpecialElements.enter().append('circle')
        newIsSpecialElements
          .call(this.applyNodeEventHandler)
        isSpecialElements.exit().remove()
        this.isSpecialElements = isSpecialElements.merge(newIsSpecialElements)
        this.isSpecialElements
          .attr('r', this.nodeRadius * 0.87)
          .attr('stroke', 'black')
          .attr('stroke-width', 2)
          .attr('fill-opacity', 0)

        const transitPreviewNodes = this.drawTransitPreviewPostset
          .concat([this.drawTransitPreviewTransition, this.drawTransitPreviewSource])
          .filter(d => d !== undefined)
        const drawTransitPreviewCircles = this.drawTransitPreviewGroup
          .selectAll('circle')
          .data(transitPreviewNodes)
        const newCircles = drawTransitPreviewCircles.enter()
          .append('circle')
        newCircles.call(this.applyNodeEventHandler)
        drawTransitPreviewCircles.exit().remove()
        this.drawTransitPreviewCircles = drawTransitPreviewCircles.merge(newCircles)
        this.drawTransitPreviewCircles
          .attr('r', d => {
            if (d.type === 'ENVPLACE' || d.type === 'SYSPLACE') {
              return this.nodeRadius * 1.4
            } else {
              return this.nodeRadius * 1.6
            }
          })
          .attr('stroke', 'black')
          .attr('stroke-width', 2)
          .attr('fill-opacity', 0.1)

        const inhibitorArcCircleElements = this.inhibitorArcCircleGroup
          .selectAll('circle')
          .data(this.links.filter(d => d.isInhibitorArc))
        const newInhibitorArcCircles = inhibitorArcCircleElements.enter()
          .append('circle')
        newInhibitorArcCircles.call(this.applyLinkEventHandler)
        inhibitorArcCircleElements.exit().remove()
        this.inhibitorArcCircleElements = inhibitorArcCircleElements.merge(newInhibitorArcCircles)
        this.inhibitorArcCircleElements
          .attr('r', 10)
          .attr('stroke', 'black')
          .attr('stroke-width', 2)
          .attr('fill', 'white')
          .attr('fill-opacity', 1)

        const nodeElements = this.nodeGroup
          .selectAll('.graph-node')
          .data(this.nodes, this.keyFunction)
        const newNodeElements = nodeElements.enter().append((node) => {
          const shape = (node.type === 'ENVPLACE' || node.type === 'SYSPLACE') ? 'circle' : 'rect'
          return document.createElementNS('http://www.w3.org/2000/svg', shape)
        })
        newNodeElements
          .call(this.applyNodeEventHandler)
        nodeElements.exit().remove()
        this.nodeElements = nodeElements.merge(newNodeElements)
        this.nodeElements
          .attr('class', d => `graph-node ${d.type}`)
          .attr('r', this.nodeRadius)
          .attr('width', this.calculateNodeWidth)
          .attr('height', this.calculateNodeHeight)
          .attr('stroke', d => {
            if (d.type === 'GRAPH_STRATEGY_BDD_STATE') {
              return d.isGood ? 'green' : 'black'
            } else {
              return 'black'
            }
          })
          .attr('stroke-dasharray', d => {
            if (d.type === 'GRAPH_STRATEGY_BDD_STATE' && d.isGood) {
              return '20,10'
            } else if ((d.type === 'ENVPLACE' || d.type === 'SYSPLACE') && d.isInitialTransit) {
              return '10,10'
            } else {
              return ''
            }
          })
          .attr('stroke-width', d => {
            if (d.type === 'GRAPH_STRATEGY_BDD_STATE') {
              return d.isBad || d.isGood ? 5 : 2
            } else {
              return 2
            }
          })
        const maxPartition = this.nodes.reduce((max, node) => node.partition > max ? node.partition : max, 0)

        function partitionColorForPlace(place) {
          const hueDegrees = place.partition / (maxPartition + 1) * 360
          console.log(`maxPartition: ${maxPartition}, place.partition: ${place.partition}, hueDegress: ${hueDegrees}`)
          const luminosity = place.type === 'SYSPLACE' ? 35 : 90
          return `HSL(${hueDegrees}, ${luminosity + 20}%, ${luminosity}%`
        }

        this.nodeElements
        // Don't mess with the color of a node if an animation is running on it
          .filter(d => !d.hasScheduledAnimation)
          .attr('fill', this.fillOfNodeElement)

        const getTransitColor = link => {
          if (link.transitHue !== undefined) {
            const hueInDegrees = link.transitHue * 360
            return `HSL(${hueInDegrees}, 100%, 50%)`
          } else {
            throw new Error(`The property transitHue is undefined for the link: ${link}`)
          }
        }
        const linkSelection = this.linkGroup
          .selectAll('g')
          .data(this.links.concat(this.drawTransitPreviewLinks))
        const linkEnter = linkSelection.enter().append('g')
        // These are the links you can actually see
        const linkEnterVisiblePath = linkEnter.append('path')
          .attr('fill', 'none')
          .attr('class', 'visibleLink')
        // These are invisible path elements that serve as big hitboxes for our thin links
        const linkEnterInvisiblePath = linkEnter.append('path')
          .attr('fill', 'none')
          .attr('stroke', '#33aacc00')
          .attr('stroke-width', 20)
          .call(this.applyLinkEventHandler)
        linkSelection.exit().remove()
        // We're passing our data down from the parent 'g' element to two child
        // 'path' elements,  so each parent datum must be mapped to an array of two child datums
        // using selection.data
        this.linkElements = linkEnter.merge(linkSelection).selectAll('path')
          .data((d) => [d, d])
        // Apply styles to the visible link elements
        this.linkElements.filter('.visibleLink')
          .attr('stroke-width', link => {
            if (this.highlightedDatum == link || link.isHovered) {
              return 5
            } else if (this.drawTransitPreviewLinks.includes(link)) {
              return 6
            } else {
              return 3
            }
          })
          .attr('stroke', link => {
            if (this.drawTransitPreviewLinks.includes(link)) {
              return '#444444'
            } else if (link.transitHue !== undefined) {
              return getTransitColor(link)
            } else {
              return '#E5E5E5'
            }
          })
          .attr('marker-end', link => {
            if (link.isInhibitorArc) {
              return ''
            } else {
              return 'url(#' + this.arrowheadId + ')'
            }
          })
          .attr('id', this.generateLinkId)

        const newLinkTextElements = this.linkTextGroup
          .selectAll('text')
          .data(this.links.filter(link => link.transitionId !== undefined || link.transit !== undefined))
        const linkTextEnter = newLinkTextElements
          .enter().append('text')
          .attr('font-size', 25)
          .call(this.applyLinkEventHandler)
        linkTextEnter.append('textPath')
          .call(this.applyLinkEventHandler)
        newLinkTextElements.exit().remove()
        this.linkTextElements = linkTextEnter.merge(newLinkTextElements)
        this.linkTextElements
          .attr('fill', link => {
            if (link.transitHue !== undefined) {
              return getTransitColor(link)
            } else {
              return 'black'
            }
          })
          .select('textPath')
          .attr('xlink:href', link => '#' + this.generateLinkId(link))
          .text(link => {
            if (link.transitionId !== undefined) {
              // This is for Graph Game BDDs
              return link.transitionId
            } else if (link.transit !== undefined) {
              // This is for Petri Games
              return link.transit
            } else {
              throw new Error('Both transitionId and transit are both undefined.')
            }
          })

        this.updateSimulation()
      },
      updateSimulation: function () {
        const drawFlowPreviewSizes = {
          'ENVPLACE': this.nodeRadius * 1.4,
          'SYSPLACE': this.nodeRadius * 1.4,
          'TRANSITION': this.nodeRadius * 1.6
        }
        // let ticksElapsed = 0 // For printing debug info
        this.physicsSimulation.nodes(this.nodes).on('tick', () => {
          // Debugging alpha value of physics simulation (used to pause it when it's not in use)
          // ticksElapsed++
          // if (ticksElapsed === 10) {
          //   ticksElapsed = 0
          //   console.log(`Simulation alpha: ${this.physicsSimulation.alpha()}`)
          // }

          // Make sure the D3 forceSimulation is only running if the Graph Editor is visible.
          const svgElement = this.$refs.svg
          const isSvgVisible = !!(svgElement.offsetWidth || svgElement.offsetHeight || svgElement.getClientRects().length)
          if (!isSvgVisible) {
            // console.log('Stopping forceSimulation for 2 seconds because GraphEditor with this UID is not visible: ' + this._uid)
            this.physicsSimulation.stop()
            setTimeout(() => {
              if (!this.isDestroyed) {
                // console.log('Restarting forceSimulation after 2 seconds')
                this.physicsSimulation.restart()
              }
            }, 2000)
            return
          }
          this.nodeElements.filter('rect')
            .attr('transform', node =>
              `translate(
              ${node.x - this.calculateNodeWidth(node) / 2},
              ${node.y - this.calculateNodeHeight(node) / 2})`)
          this.nodeElements.filter('circle')
            .attr('transform', node => `translate(${node.x},${node.y})`)
          this.isSpecialElements
            .attr('transform', node => `translate(${node.x},${node.y})`)
          this.labelElements
            .attr('x', node => node.x)
            // TODO Use function mentioned in other TODO to determine where the bottom of the node is
            .attr('y', node => node.y + this.calculateNodeHeight(node) / 2 + 15)
          this.contentElements
            .attr('x', node => node.x)
            .attr('y', node => {
              if (node.type === 'GRAPH_STRATEGY_BDD_STATE') {
                return node.y - this.calculateNodeHeight(node) / 2 + 30
              } else {
                return node.y - this.calculateNodeHeight(node) / 2 + 38
              }
            })
          this.drawFlowPreview
            .attr('transform', () => {
              const target = this.drawFlowTarget
              if (target) {
                return `translate(${target.x},${target.y})`
              } else {
                return ''
              }
            })
            .attr('r', () => {
              const target = this.drawFlowTarget
              if (target) {
                return drawFlowPreviewSizes[target.type]
              } else {
                return 0
              }
            })
          this.drawTransitPreviewCircles
            .attr('transform', d => {
              return `translate(${d.x},${d.y})`
            })

          // TODO implement super cool feature to stretch and shrink a selection (moving all nodes proportionally)
          // But for now don't use this function because the border, if drawn incorrectly, messes
          // up the "zoom to fit all nodes" functionality
          // this.updateSelectionBorder()

          // Update links to match the nodes' new positions
          this.linkElements
            .attr('d', d => {
              const unadjustedPathD = pathForLink.call(this, d)
              if (!d.isInhibitorArc) {
                return unadjustedPathD
              } else {
                const inhibitorArcCircleRadius = 10
                // Back off the arc by a small distance to make room for a little circle in between
                // the arrowhead and the target node
                // We do this by constructing a SVG element with the given 'unadjusted path'
                // and using its 'getPointAtLength' method to figure out where the actual path
                // should end
                const pathEl = document.createElementNS('http://www.w3.org/2000/svg', 'path')
                pathEl.setAttributeNS(null, 'd', unadjustedPathD)
                const length = pathEl.getTotalLength()
                const endpoint = pathEl.getPointAtLength(length - inhibitorArcCircleRadius * 2)

                // Save this for later in order to place the inhibitor arc circle
                d.inhibitorArcCircleCenter =
                  pathEl.getPointAtLength(length - inhibitorArcCircleRadius)

                return pathForLink.call(this, d, {endpoint})
                // console.log('path length: ' + length)
                // console.log('point before end: ')
                // console.log(point)
              }
            })

          // TODO Position inhibitor arc circles
          // (I guess it makes sense to create/destroy them inside of updateD3 and then position
          // them here.)
          this.inhibitorArcCircleElements.attr('transform',
            (d) => `translate(${d.inhibitorArcCircleCenter.x},${d.inhibitorArcCircleCenter.y})`
          )

          // Position link labels at the center of the links based on the distance calculated above
          this.linkTextElements
            .attr('dx', d => {
              return d.pathLength / 2
            })

          // Let the physics simulation know what links it is working with
          this.physicsSimulation.force('link').links(this.links)
        })
        // Raise the temperature of the force simulation and restart it, because if the simulation
        // isn't running, the newly inserted nodes' positions will not get applied to the SVG,
        // and you won't be able to see them.
        this.physicsSimulation.alpha(0.7).restart()
      },
      updateSelectionBorder: function () {
        // TODO Figure out why this sometimes yields a border from -999999 to 999999
        if (this.selectedNodes.length === 0) {
          this.selectionBorder.attr('d', '')
        } else {
          const domNodeElements = this.nodeElements
            .filter(this.selectedNodes.includes)
            .nodes()
          const border = containingBorder(domNodeElements)
          const path = rectanglePath(...border)
          this.selectionBorder.attr('d', path)
        }
      },
      /**
       * Perform a diff of the new graph against our existing graph, updating our graph in-place.
       * Delete nodes/links that are gone, update nodes that have changed, add new nodes/links.
       * TODO Validate the graphJson; make sure it has all the properties we expect to see.
       */
      importGraph: function (graphJson) {
        const graphJsonCopy = this.deepCopy(graphJson)
        this.winningCondition = graphJsonCopy.winningCondition
        // There is only a ltlFormula sent from server to client iff winningCondition != LTL.
        // (Other winning conditions get translated to LTL formulas using  e.g. AdamModelChecker.toFlowLTLFormula.)
        if (typeof graphJsonCopy.ltlFormula === 'string' && graphJsonCopy.ltlFormula !== '') {
          this.ltlFormula = graphJsonCopy.ltlFormula
        }
        const newLinks = graphJsonCopy.links
        const newNodes = graphJsonCopy.nodes
        const newNodePositions = graphJsonCopy.nodePositions

        // Delete the nodes that are no longer present, and update the ones that are still present
        this.nodes = this.nodes.filter(oldNode => {
          const newEquivalentNode = newNodes.find(newNode => oldNode.id === newNode.id)
          const nodeIsStillPresent = newEquivalentNode !== undefined
          if (nodeIsStillPresent) {
            // Update the node's id, label, etc. while retaining x/y coordinates
            Object.assign(oldNode, newEquivalentNode)
          }
          return nodeIsStillPresent
        })

        // Add the new nodes into our nodes array
        newNodes.forEach(newNode => {
          const nodeIsAlreadyPresent = this.nodes.some(oldNode => oldNode.id === newNode.id)
          if (!nodeIsAlreadyPresent) {
            // TODO Consider fixing nodes for which "isPostsetExpanded" is true.  Right now, nodes tend to
            // get pushed around in a disorienting way when their children are added to the graph.
            // Randomize the position slightly to stop the nodes from flying away from each other
            newNode.x = this.nodeSpawnPoint.x + (Math.random() - 0.5) * 40
            newNode.y = this.nodeSpawnPoint.y + (Math.random() - 0.5) * 40
            this.nodes.push(newNode)
          }
        })

        // Apply the x/y coordinates that are given to us by the server
        // Note that this freezes the nodes.
        // TODO Consider allowing some nodes to be frozen and others to be free-floating
        // TODO (This would require a boolean AdamExtension to be added)
        this.nodes.forEach(node => {
          if (newNodePositions.hasOwnProperty(node.id)) {
            // logging.logVerbose('updating x/y coordinates of this node: ' + node.id)
            const newPosition = newNodePositions[node.id]
            node.fx = newPosition.x
            node.fy = newPosition.y
          }
        })

        // Delete the links that are no longer present
        this.links = this.links.filter(oldLink => {
          newLinks.some(newLink => {
            const sourceMatches = oldLink.source.id === newLink.source
            const targetMatches = oldLink.target.id === newLink.target
            return sourceMatches && targetMatches
          })
        })

        // Add the new links into our links array
        newLinks.forEach(newLink => {
          const linkIsAlreadyPresent = this.links.some(oldLink => {
            const sourceMatches = oldLink.source.id === newLink.source
            const targetMatches = oldLink.target.id === newLink.target
            return sourceMatches && targetMatches
          })
          if (!linkIsAlreadyPresent) {
            // The way D3's force layout works is, if you want, you can supply it an array of links
            // where each link just contains the ID of its source and target nodes.
            // If you do this, D3 will automatically replace each ID with a reference to the node.
            // This works great for a graph that never changes.
            // But if you later want to update the graph, and you have a mixture of references and
            // IDs in your links array, D3 will go through all the links and replace all the
            // references again for you. This makes all the links disappear for a fraction
            // of a second and then reappear.  It's an unsettling visual effect.
            // To prevent this from happening, we manually replace the IDs with references ourselves,
            // saving D3 the effort of doing it for us.
            const newLinkWithReferences = Object.assign(newLink, {
              source: this.nodes.find(node => node.id === newLink.source),
              target: this.nodes.find(node => node.id === newLink.target)
            })
            this.links.push(newLinkWithReferences)
          }
        })
        // TODO document how this works.  It's a fiddly bit of state management to ensure that
        // nodes spawn under the mouse cursor if triggered by a user clicking, but otherwise, they
        // should spawn at the center of the SVG (e.g. upon editing the APT).
        // Maybe a better solution would be to send to the server the x/y coordinates of the click
        // so that they can be automatically added to the new nodes that are created by the click.
        // (At the time of writing (23.04.2018), the only nodes that are added by clicking are Graph
        // Game BDD States when a State is clicked on to show its preset/postset.
        this.lastUserClick = undefined
      },
      keyFunction: function (data) {
        return `${data.id}::${data.type}`
      },
      generateLinkId: function (link) {
        return `${this._uid}${link.source.id}::${link.target.id}`
      },
      calculateNodeWidth: function (d) {
        if (d.content !== undefined) {
          return d.maxContentLineLength * 8 + 10
          // return 125 // TODO Make width expand to fit text (use fixed width font if necessary)
        } else {
          return this.nodeRadius * 2
        }
      },
      calculateNodeHeight: function (d) {
        if (d.content !== undefined) {
          return d.numberOfLines * 20
          // return 90 // TODO Make height expand to fit text
        } else {
          return this.nodeRadius * 2
        }
      },
      fillOfNodeElement: function (data) {
        const isHighlightedOrHovered = this.highlightedDatum == data || data.isHovered
        const isSelected = this.selectedNodes.includes(data)
        if (isHighlightedOrHovered && isSelected) {
          return '#3366bb'
        } else if (isHighlightedOrHovered || isSelected) {
          return '#99aadd'
        } else if (this.shouldShowPartitions && data.partition !== -1) {
          return partitionColorForPlace(data)
        } else if (data.type === 'ENVPLACE') {
          return 'white'
        } else if (this.useModelChecking && data.type === 'SYSPLACE') {
          return 'white' // In Model Checking mode, all places should be white.
        } else if (data.type === 'SYSPLACE') {
          return 'lightgrey'
        } else if (data.type === 'TRANSITION') {
          switch (data.fairness) {
            case 'weak':
              return '#8888FF'
            case 'strong':
              return '#3333FF'
            default:
              return 'white'
          }
        } else if (data.type === 'GRAPH_STRATEGY_BDD_STATE') {
          return data.isMcut ? 'white' : 'lightgrey'
        } else {
          return 'black' // TODO Throw some kind of exception or error.  This should be an exhaustive pattern match
        }
      },
      svgWidth: function () {
        return this.svg.node().getBoundingClientRect().width
      },
      svgHeight: function () {
        return this.svg.node().getBoundingClientRect().height
      },
      updateGravityStrength: function (strength) {
        this.physicsSimulation.force('gravity').strength(strength)
        this.physicsSimulation.alpha(0.7).restart()
      },
      updateLinkStrength: function (strength) {
        this.physicsSimulation.force('link').strength(strength)
        this.physicsSimulation.alpha(0.7).restart()
      },
      updateRepulsionStrength: function (strength) {
        this.physicsSimulation.force('charge').strength(-strength)
        this.physicsSimulation.alpha(0.7).restart()
      },
      /**
       * Perform a deep copy of an arbitrary object.
       * This has some caveats.
       * See https://stackoverflow.com/questions/20662319/javascript-deep-copy-using-json
       * TODO: Consider refactoring this out into its own little module if a similar trick is used in other components.
       * @param object
       * @returns A deep copy/clone of object
       */
      deepCopy: function (object) {
        return JSON.parse(JSON.stringify(object))
      },
      // TODO Explain what this function is for
      calculateNodeOffset: function (data) {
        if (data.type === 'ENVPLACE') {
          // Node is a circle
          return this.nodeRadius
        } else if (data.type === 'SYSPLACE') {
          return this.nodeRadius
        } else if (data.type === 'TRANSITION') {
          // Node is a rectangle
          return this.calculateNodeHeight(data)
        } else if (data.type === 'GRAPH_STRATEGY_BDD_STATE') {
          return this.calculateNodeHeight(data)
        }
      },
      contextMenuItemsTransition: function (transitionNode) {
        const setWeakFair = {
          title: 'Set weak fair',
          action: (d) => this.$emit('setFairness', {
            transitionId: d.id,
            fairness: 'weak'
          })
        }
        const setStrongFair = {
          title: 'Set strong fair',
          action: (d) => this.$emit('setFairness', {
            transitionId: d.id,
            fairness: 'strong'
          })
        }
        const removeFairness = {
          title: 'Remove fairness',
          action: (d) => this.$emit('setFairness', {
            transitionId: d.id,
            fairness: 'none'
          })
        }
        const fireTransition = {
          title: 'Fire transition',
          action: this.fireTransition
        }
        switch (transitionNode.fairness) {
          case 'weak':
            return [fireTransition, setStrongFair, removeFairness]
          case 'strong':
            return [fireTransition, setWeakFair, removeFairness]
          case 'none':
            return [fireTransition, setWeakFair, setStrongFair]
          default:
            throw new Error(
              `Invalid value for fairness for the transition node ${transitionNode.id}:
              ${transitionNode.fairness}`)
        }
      }
    }
  }

</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>

  /* inconsolata-regular - latin */
  @font-face {
    font-family: 'Inconsolata';
    font-style: normal;
    font-weight: 400;
    src: local('Inconsolata Regular'), local('Inconsolata-Regular'),
    url('../assets/fonts/inconsolata-v16-latin-regular.woff2') format('woff2'), /* Chrome 26+, Opera 23+, Firefox 39+ */ url('../assets/fonts/inconsolata-v16-latin-regular.woff') format('woff'); /* Chrome 6+, Firefox 3.6+, IE 9+, Safari 5.1+ */
  }

  /* inconsolata-700 - latin */
  @font-face {
    font-family: 'Inconsolata';
    font-style: normal;
    font-weight: 700;
    src: local('Inconsolata Bold'), local('Inconsolata-Bold'),
    url('../assets/fonts/inconsolata-v16-latin-700.woff2') format('woff2'), /* Chrome 26+, Opera 23+, Firefox 39+ */ url('../assets/fonts/inconsolata-v16-latin-700.woff') format('woff'); /* Chrome 6+, Firefox 3.6+, IE 9+, Safari 5.1+ */
  }


  .graph-editor {
    /*TODO Make the graph editor use up exactly as much space as is given to it.*/
    /*For some reason, when I set this to 100%,it does not grow to fill the space available.*/
    height: 100%;
    width: 100%;
    max-width: 100%;
    display: relative;
    background-color: #fafafa;
  }

  .graph {
    flex-grow: 1;
    text-align: left;
  }

  .graph-editor-toolbar {
    width: 100%;
    display: flex;
    flex-direction: row;
    align-items: center;
    padding: 5px;
  }

  .graph-editor-toolbar button,
  .graph-editor-toolbar input {
    margin: 5px;
  }

  .forceStrengthSlider {
    flex-grow: 0.5;
  }

  .forceStrengthNumber {
    width: 80px;
    padding-left: 10px;
  }
</style>
