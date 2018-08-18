<template>
  <!--The attribute tabIndex is here to allow the div to receive keyboard focus.-->
  <div class="graph-editor" :id="rootElementId" ref="rootElement" :tabIndex="-1">
    <link href="https://fonts.googleapis.com/css?family=Inconsolata" rel="stylesheet">
    <div
      style="position: absolute; width: 100%; padding-right: 20px; z-index: 2; background-color: #fafafa"
      ref="toolbarContainer">
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
      <div class="graph-editor-toolbar">
        <button v-on:click="autoLayout(); freezeAllNodes()">Auto-Layout</button>
        <button style="margin-right: auto" v-if="shouldShowSaveAPTButton"
                v-on:click="saveGraphAsAPT">
          Save graph as APT
        </button>
        <button style="margin-left: auto" v-on:click="zoomToFitAllNodes">
          Zoom to fit all nodes
        </button>
        <button style="margin-left: auto" v-on:click="moveNodesToVisibleArea">
          Move all nodes to visible area
        </button>
        <button style="display: none;" v-on:click="updateD3">Update D3</button>
        <button v-on:click="freezeAllNodes">Freeze all nodes</button>
        <button class="btn-danger" v-on:click="unfreezeAllNodes">Unfreeze all nodes</button>
        <button class="btn-danger" v-on:click="deleteSelectedNodes">Delete selected nodes</button>
        <button v-on:click="invertSelection">Invert selection</button>
      </div>
      <div class="graph-editor-toolbar">
        <v-radio-group v-model="selectedTool" row>
          <v-radio label="select" value="select"/>
          <v-radio label="draw flows" value="drawFlow"/>
          <v-radio label="insert sysplace" value="insertSysPlace"/>
          <v-radio label="insert envplace" value="insertEnvPlace"/>
          <v-radio label="insert transition" value="insertTransition"/>
          <v-radio label="delete node" value="deleteNode"/>
        </v-radio-group>
      </div>
    </div>

    <svg class='graph' :id='this.graphSvgId' style="position: absolute; z-index: 0;">

    </svg>
    <v-radio-group v-model="nodeTypeToInsert" style="position: relative; top: 220px; width: 150px;">
      <v-radio label="SYSPLACE" value="SYSPLACE"/>
      <v-radio label="ENVPLACE" value="ENVPLACE"/>
      <v-radio label="TRANSITION" value="TRANSITION"/>
    </v-radio-group>
    <v-radio-group v-model="dragDropMode" style="position: relative; top: 180px; width: 150px;">
      <v-radio label="move nodes" value="moveNode"/>
      <v-radio label="draw flows" value="drawFlow"/>
    </v-radio-group>
    <v-radio-group v-model="leftClickMode" style="position: relative; top: 140px; width: 150px;">
      <v-radio label="unfreeze nodes" value="unfreezeNode"/>
      <v-radio label="delete nodes" value="deleteNode"/>
      <v-radio label="select node" value="selectNode"/>
    </v-radio-group>
    <v-radio-group v-model="backgroundClickMode" style="position: relative; top: 120px; width: 150px;">
      <v-radio label="cancel selection" value="cancelSelection"/>
      <v-radio label="insert node" value="insertNode"/>
    </v-radio-group>
    <v-radio-group v-model="backgroundDragDropMode" style="position: relative; top: 100px; width: 150px;">
      <v-radio label="zoom and pan" value="zoom"/>
      <v-radio label="select nodes" value="selectNodes"/>
    </v-radio-group>
  </div>
</template>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
  .graph-editor {
    /*TODO Make the graph editor use up exactly as much space as is given to it.*/
    /*For some reason, when I set this to 100%,it does not grow to fill the space available.*/
    height: 100%;
    width: 100%;
    max-width: 100%;
    display: flex;
    flex-direction: column;
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
    font-size: 20px;
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

<script>
  import * as d3 from 'd3'
  import { saveFileAs } from '@/fileutilities'
  import { layoutNodes } from '@/autoLayout'
  import { pointOnRect, pointOnCircle } from '@/shapeIntersections'
  import { rectanglePath, arcPath, loopPath, containingBorder } from '../svgFunctions'
  import 'd3-context-menu/css/d3-context-menu.css'
  import contextMenuFactory from 'd3-context-menu'

  // Polyfill for IntersectionObserver API.  Used to detect whether graph is visible or not.
  require('intersection-observer')

  export default {
    name: 'graph-editor',
    components: {},
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
        console.log(event)
        switch (event.keyCode) {
          case 27:
            this.selectedNodesIds = []
            break // 'Esc' key
          case 46:
            this.deleteSelectedNodes()
            break // 'delete' key
        }
      })
    },
    props: {
      graph: {
        type: Object,
        required: true
      },
      dimensions: {
        type: Object,
        required: true
      },
      shouldShowPhysicsControls: {
        type: Boolean,
        default: false
      },
      shouldShowSaveAPTButton: {
        type: Boolean,
        default: false
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
    computed: {
      closeContextMenu: function () {
        return () => {
          contextMenuFactory('close')
        }
      },
      openContextMenu: function () {
        return contextMenuFactory(this.contextMenuItems)
      },
      contextMenuItems: function () {
        return [
          {
            title: function (d) {
              switch (d.type) {
                case 'SYSPLACE':
                case 'ENVPLACE':
                  return `Delete Place ${d.id}`
                case 'TRANSITION':
                  return `Delete Transition ${d.id}`
                default:
                  throw new Error('Unhandled case in switch statement for right click context menu')
              }
            },
            action: function (d) {
              this.$emit('deleteNode', d.id)
            },
            disabled: false // optional, defaults to false
          },
          {
            title: 'Item #2',
            disabled: true
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
      rootElementId: function () {
        return 'graph-editor-' + this._uid
      },
      graphSvgId: function () {
        return 'graph-' + this._uid
      },
      arrowheadId: function () {
        return 'arrowhead-' + this._uid
      },
      // Given a d3 selection, apply to it all of the event handlers that we want nodes to have.
      // Usage: selection.call(applyNodeEventHandler)
      applyNodeEventHandler: function () {
        return selection => {
          this.dragDrop(selection)
          selection.on('click', this.onNodeClick)
          selection.on('contextmenu', this.onNodeRightClick)
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
                if (this.selectedNodesIds.includes(d.id)) {
                  this.selectedNodesIds = this.selectedNodesIds.filter(id => id !== d.id)
                } else {
                  this.selectedNodesIds.push(d.id)
                }
              } else {
                this.selectedNodesIds = [d.id]
              }
            }
          default:
            return () => {
              console.log(`No left click handler was found for leftClickMode === ${this.leftClickMode}`)
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
              this.selectedNodesIds = []
            }
          case 'cancelSelection':
            return () => {
              this.selectedNodesIds = []
            }
          default:
            return () => {
              console.log(`No background click handler found for backgroundClickMode === ${this.backgroundClickMode}`)
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
        return {
          'start': node => {
            isSelectionDrag = this.selectedNodesIds.includes(node.id)
            node.fx = node.x
            node.fy = node.y
          },
          'drag': node => {
            this.simulation.alphaTarget(0.7).restart()
            if (isSelectionDrag) {
              this.nodes.forEach(node => {
                if (this.selectedNodesIds.includes(node.id)) {
                  node.fx = node.x + d3.event.dx
                  node.fy = node.y + d3.event.dy
                }
              })
            } else {
              node.fx = d3.event.x
              node.fy = d3.event.y
            }
          },
          'end': node => {
            if (!d3.event.active) {
              this.simulation.alphaTarget(0)
            }
            this.onGraphModified()
          }
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
            // TODO Figure out a way to specify token flows

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
        function findFlowTarget (mousePos, startNode, nodes, links) {
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
          function isEligible (node) {
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
        return d3.drag()
          .clickDistance(2)
          .on('start', () => {
            this.closeContextMenu();
            [startX, startY] = this.mousePosZoom()
          })
          .on('drag', () => {
            const [currentX, currentY] = this.mousePosZoom()
            this.selectNodesPreview
              .attr('d', rectanglePath(startX, startY, currentX, currentY))
            this.selectedNodesIds = findSelectedNodes(this.nodes, startX, startY, currentX, currentY)
              .map(node => node.id)
          })
          .on('end', () => {
            const [currentX, currentY] = this.mousePosZoom()
            console.log(`did a drag drop on the background from ${startX},${startY} to ${currentX}, ${currentY}`)
            console.log('selected nodes:')
            console.log(this.selectedNodesIds)
            this.selectNodesPreview.attr('d', '')
          })

        function findSelectedNodes (nodes, startX, startY, currentX, currentY) {
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
      selectedTool: function (tool) {
        switch (tool) {
          case 'select': {
            this.backgroundClickMode = 'cancelSelection'
            this.backgroundDragDropMode = 'selectNodes'
            this.leftClickMode = 'selectNode'
            this.dragDropMode = 'moveNode'
            break
          }
          case 'drawFlow': {
            this.backgroundDragDropMode = 'zoom'
            this.backgroundClickMode = 'cancelSelection'
            this.leftClickMode = 'selectNode'
            this.dragDropMode = 'drawFlow'
            break
          }
          case 'insertSysPlace': {
            this.backgroundDragDropMode = 'selectNodes'
            this.backgroundClickMode = 'insertNode'
            this.leftClickMode = 'selectNode'
            this.dragDropMode = 'moveNode'
            this.nodeTypeToInsert = 'SYSPLACE'
            break
          }
          case 'insertEnvPlace': {
            this.backgroundDragDropMode = 'selectNodes'
            this.backgroundClickMode = 'insertNode'
            this.leftClickMode = 'selectNode'
            this.dragDropMode = 'moveNode'
            this.nodeTypeToInsert = 'ENVPLACE'
            break
          }
          case 'insertTransition': {
            this.backgroundDragDropMode = 'selectNodes'
            this.backgroundClickMode = 'insertNode'
            this.leftClickMode = 'selectNode'
            this.dragDropMode = 'moveNode'
            this.nodeTypeToInsert = 'TRANSITION'
            break
          }
          case 'deleteNode': {
            this.backgroundDragDropMode = 'selectNodes'
            this.backgroundClickMode = 'cancelSelection'
            this.leftClickMode = 'deleteNode'
            this.dragDropMode = 'moveNode'
            break
          }
        }
      },
      selectedNodesIds: function () {
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
      dimensions: function (dims) {
        this.updateSvgDimensions()
      }
    },
    data () {
      return {
        // TODO consider using a set instead of an array to prevent bugs from happening
        selectedNodesIds: [],
        selectedTool: 'select',
        backgroundClickMode: 'cancelSelection',
        backgroundDragDropMode: 'selectNodes',
        leftClickMode: 'selectNode',
        dragDropMode: 'moveNode',
        nodeTypeToInsert: 'SYSPLACE',
        saveGraphAPTRequestPreview: {},
        nodeRadius: 27,
        exportedGraphJson: {},
        svg: undefined,
        linkGroup: undefined,
        linkTextGroup: undefined,
        nodeGroup: undefined,
        labelGroup: undefined,
        contentGroup: undefined,
        isSpecialElements: undefined,
        nodeElements: undefined,
        linkElements: undefined,
        linkTextElements: undefined,
        labelElements: undefined,
        contentElements: undefined,
        lastUserClick: undefined,
        simulation: d3.forceSimulation()
          .force('gravity', d3.forceManyBody().distanceMin(1000))
          .force('charge', d3.forceManyBody())
          //          .force('center', d3.forceCenter(width / 2, height / 2))
          .force('link', d3.forceLink()
            .id(link => link.id))
          .alphaMin(0.002),
        repulsionStrength: this.repulsionStrengthDefault,
        linkStrength: this.linkStrengthDefault,
        gravityStrength: this.gravityStrengthDefault
      }
    },
    methods: {
      invertSelection: function () {
        this.selectedNodesIds = this.nodes.filter(node => !this.selectedNodesIds.includes(node.id))
          .map(node => node.id)
      },
      deleteSelectedNodes: function () {
        console.log('deleting selected nodes')
        this.selectedNodesIds.forEach(id => {
          this.$emit('deleteNode', id)
        })
        this.selectedNodesIds = []
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
        // TODO replace hack with proper solution (this is highly specific to my weird tabs component)
        this.svg.attr('height', `${this.dimensions.height - 59 - 28 + 24}px`)
        this.updateCenterForce()
      },
      /**
       * We try to keep the Petri Net centered in the middle of the viewing area by applying a force to it.
       */
      updateCenterForce: function () {
        const transform = d3.zoomTransform(this.svg.node())
        const centerX = transform.invertX(this.dimensions.width / 2)
        const centerY = transform.invertY(this.dimensions.height / 2)
        console.log(`Updating center force to coordinates: ${centerX}, ${centerY}`)
        // forceCenter is an alternative to forceX/forceY.  It works in a different way.  See D3's documentation.
        // this.simulation.force('center', d3.forceCenter(svgX / 2, svgY / 2))
        const centerStrength = 0.01
        this.simulation.force('centerX', d3.forceX(centerX).strength(centerStrength))
        this.simulation.force('centerY', d3.forceY(centerY).strength(centerStrength))
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
      },
      // If you lost track of where the graph actually is, you can click this button and it will
      // zoom out far enough that all the nodes can be seen.  :)
      zoomToFitAllNodes: function () {
        const bbox = this.container.node().getBBox()
        const svgWidth = this.svgWidth()
        const toolbarHeight = this.$refs.toolbarContainer.clientHeight
        const svgHeight = this.svgHeight() - toolbarHeight
        const containerCenter = [
          bbox.x + bbox.width / 2,
          bbox.y + bbox.height / 2]
        const scale = 0.9 / Math.max(bbox.width / svgWidth, bbox.height / svgHeight)
        const translate = [
          svgWidth / 2 - scale * containerCenter[0],
          svgHeight / 2 - scale * containerCenter[1] + toolbarHeight]
        this.svg.transition().duration(300).call(this.zoom.transform, d3.zoomIdentity.translate(translate[0], translate[1]).scale(scale))
      },
      // Sometimes nodes might get lost outside the borders of the screen.
      // This procedure places them back within the visible area.
      moveNodesToVisibleArea: function () {
        const margin = 45
        const boundingRect = this.svg.node().getBoundingClientRect()
        const toolbarHeight = this.$refs.toolbarContainer.clientHeight
        // There is a transformation applied to the SVG container using d3-zoom.
        // Calculate the actual visible area's margins using the inverse of the transform.
        const transform = d3.zoomTransform(this.svg.node())

        const minX = transform.invertX(margin)
        const maxX = transform.invertX(boundingRect.width - margin)
        const minY = transform.invertY(margin + toolbarHeight)
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
      },
      saveGraphAsAPT: function () {
        this.freezeAllNodes()
        // Convert our array of nodes to a map with node IDs as keys and x,y coordinates as value.
        const mapNodeIDXY = this.nodes.reduce(function (map, node) {
          map[node.id] = {
            x: node.x.toFixed(2),
            y: node.y.toFixed(2)
          }
          return map
        }, {})
        this.saveGraphAPTRequestPreview = JSON.stringify(mapNodeIDXY, null, 2)
        this.$emit('saveGraphAsAPT', mapNodeIDXY)
      },
      onGraphModified: function () {
        // TODO Consider this as a possible culprit if there prove to be memory leaks or other performance problems.
        const graph = {
          links: this.deepCopy(this.links),
          nodes: this.deepCopy(this.nodes)
        }
        this.$emit('graphModified', graph)
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
        this.svg.on('click', d => {
          this.closeContextMenu()
          this.backgroundClickHandler(d)
        })
        this.backgroundDragDrop(this.svg)

        this.container = this.svg.append('g')
        this.linkGroup = this.container.append('g').attr('class', 'links')
        this.linkTextGroup = this.container.append('g').attr('class', 'linkTexts')
        this.nodeGroup = this.container.append('g').attr('class', 'nodes')
        this.isSpecialGroup = this.container.append('g').attr('class', 'isSpecialHighlights')
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
          .attr('stroke-width', 5)

        console.log('force simulation minimum alpha value: ' + this.simulation.alphaMin())

        this.updateD3()

//        d3.selectAll('*').on('click', function (d) { console.log(d) })
      },
      /**
       * This method should be called every time the "nodes" or "links" arrays are updated.
       * It causes our visualization to update accordingly, showing new nodes and removing deleted ones.
       */
      updateD3: function () {
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
          .attr('font-size', 15)
          .text(node => {
            if (node.type === 'GRAPH_STRATEGY_BDD_STATE') {
              // Figure out how long the widest line of the content is to determine node width later
              const lines = node.content.split('\n')
              const numberOfLines = lines.length
              node.numberOfLines = numberOfLines
              const lengthsOfLines = lines.map(str => str.length)
              const maxLineLength = lengthsOfLines.reduce((max, val) => val > max ? val : max, 0)
              node.maxContentLineLength = maxLineLength
              console.log(`max content line length: ${maxLineLength}`)
              return node.content
            } else if (node.type === 'ENVPLACE' || node.type === 'SYSPLACE') {
              return node.initialToken === 0 ? '' : node.initialToken
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
          .attr('r', this.nodeRadius * 0.89)
          .attr('stroke', 'black')
          .attr('stroke-width', 2)
          .attr('fill-opacity', 0)

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
          .attr('fill', data => {
            if (this.selectedNodesIds.includes(data.id)) {
              return '#5555FF'
            } else if (data.type === 'ENVPLACE') {
              return 'white'
            } else if (data.type === 'SYSPLACE') {
              return 'lightgrey'
            } else if (data.type === 'TRANSITION') {
              return 'white'
            } else if (data.type === 'GRAPH_STRATEGY_BDD_STATE') {
              return data.isMcut ? 'white' : 'lightgrey'
            } else {
              return 'black' // TODO Throw some kind of exception or error.  This should be an exhaustive pattern match
            }
          })

        const getTokenFlowColor = link => {
          if (link.tokenFlowHue !== undefined) {
            const hueInDegrees = link.tokenFlowHue * 360
            return `HSL(${hueInDegrees}, 100%, 50%)`
          } else {
            throw new Error(`The property tokenFlowHue is undefined for the link: ${link}`)
          }
        }
        const newLinkElements = this.linkGroup
          .selectAll('path')
          .data(this.links)
        const linkEnter = newLinkElements
          .enter().append('path')
          .attr('fill', 'none')
        newLinkElements.exit().remove()
        this.linkElements = linkEnter.merge(newLinkElements)
          .attr('stroke-width', 3)
          .attr('stroke', link => {
            if (link.tokenFlowHue !== undefined) {
              return getTokenFlowColor(link)
            } else {
              return '#E5E5E5'
            }
          })
          .attr('marker-end', 'url(#' + this.arrowheadId + ')')
          .attr('id', this.generateLinkId)

        const newLinkTextElements = this.linkTextGroup
          .selectAll('text')
          .data(this.links.filter(link => link.transitionId !== undefined || link.tokenFlow !== undefined))
        const linkTextEnter = newLinkTextElements
          .enter().append('text')
          .attr('font-size', 25)
        linkTextEnter.append('textPath')
        newLinkTextElements.exit().remove()
        this.linkTextElements = linkTextEnter.merge(newLinkTextElements)
        this.linkTextElements
          .attr('fill', link => {
            if (link.tokenFlowHue !== undefined) {
              return getTokenFlowColor(link)
            } else {
              return 'black'
            }
          })
          .select('textPath')
          .attr('xlink:href', link => '#' + this.generateLinkId(link))
          .text(link => {
            if (link.transitionId !== undefined) {
              return link.transitionId
            } else if (link.tokenFlow !== undefined) {
              return link.tokenFlow
            } else {
              throw new Error('Both transitionId and tokenFlow are both undefined.')
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
        this.simulation.nodes(this.nodes).on('tick', () => {
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
            .attr('y', node => node.y - this.calculateNodeHeight(node) / 2 + 30)
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

          this.updateSelectionBorder()

          // Draw a line from the edge of one node to the edge of another.
          // We have to do this so that the arrowheads will be correctly aligned for nodes of varying size.
          // TODO Consider using https://www.npmjs.com/package/svg-intersections for more accurate results
          this.linkElements
            .attr('d', d => {
              let targetPoint
              switch (d.target.type) {
                case 'ENVPLACE':
                case 'SYSPLACE': {
                  // The target node is a circle.
                  targetPoint = pointOnCircle(
                    d.source.x,
                    d.source.y,
                    this.nodeRadius,
                    d.target.x,
                    d.target.y,
                    false)
                  break
                }
                case 'TRANSITION':
                case 'GRAPH_STRATEGY_BDD_STATE': {
                  // The target node is a rectangle.
                  targetPoint = pointOnRect(
                    d.source.x,
                    d.source.y,
                    d.target.x - this.calculateNodeWidth(d.target) / 2,
                    d.target.y - this.calculateNodeHeight(d.target) / 2,
                    d.target.x + this.calculateNodeWidth(d.target) / 2,
                    d.target.y + this.calculateNodeHeight(d.target) / 2,
                    false
                  )
                }
              }
              const targetX = targetPoint['x']
              const targetY = targetPoint['y']
              // Save the length of the link in order to place a label at its midpoint (see below)
              const dx = targetX - d.source.x
              const dy = targetY - d.source.y
              d.pathLength = Math.sqrt(dx * dx + dy * dy)

              // This means source -> target and target -> source are both links
              const multipleLinksBetweenNodes = this.links.find(link => link !== d && link.source === d.target && link.target === d.source)
              const linkIsLoop = d.target === d.source
              const isStraightLink = !multipleLinksBetweenNodes && !linkIsLoop
              if (isStraightLink) {
                // Straight line for a single edge between two distinct nodes
                return `M${d.source.x},${d.source.y} L${targetX},${targetY}`
              } else if (multipleLinksBetweenNodes) {
                return arcPath(d.source.x, d.source.y, targetX, targetY)
              } else if (linkIsLoop) {
                // Place the loop around the upper-right corner of the node.
                const x1 = d.source.x + this.calculateNodeWidth(d.source) / 2
                const y1 = d.source.y
                const x2 = d.source.x
                const y2 = d.source.y - this.calculateNodeHeight(d.source) / 2
                return loopPath(x1, y1, x2, y2)
              }
            })
          // Position link labels at the center of the links based on the distance calculated above
          this.linkTextElements
            .attr('dx', d => d.pathLength / 2)

          // Let the simulation know what links it is working with
          this.simulation.force('link').links(this.links)

          // Raise the temperature of the force simulation, because otherwise, if the temperature is
          // below alphaMin, the newly inserted nodes' positions will not get updated, and they will
          // appear in the upper left corner of the svg until something causes the temperature to
          // increase again past the threshold.
          this.simulation.alpha(0.7)
        })
      },
      updateSelectionBorder: function () {
        if (this.selectedNodesIds.length === 0) {
          this.selectionBorder.attr('d', '')
        } else {
          const domNodeElements = this.nodeElements
            .filter(node => this.selectedNodesIds.includes(node.id))
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
        return `${link.source.id}::${link.target.id}`
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
      svgWidth: function () {
        return this.svg.node().getBoundingClientRect().width
      },
      svgHeight: function () {
        return this.svg.node().getBoundingClientRect().height
      },
      updateGravityStrength: function (strength) {
        this.simulation.force('gravity').strength(strength)
      },
      updateLinkStrength: function (strength) {
        this.simulation.force('link').strength(strength)
      },
      updateRepulsionStrength: function (strength) {
        this.simulation.force('charge').strength(-strength)
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
      }
    }
  }

</script>
