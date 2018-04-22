<template>
  <div class="graph-editor" :id="rootElementId">
    <div style="position: absolute; width: 100%; padding-right: 20px; z-index: 2;">
      <div class="graph-editor-toolbar" v-if="shouldShowPhysicsControls">
        Repulsion Strength
        <input type="range" min="30" max="1000" step="1"
               class="forceStrengthSlider"
               v-model="repulsionStrength">
        <input type="number" min="30" max="1000" step="1"
               class="forceStrengthNumber"
               v-model="repulsionStrength">
        Link strength
        <input type="range" min="0" max="0.2" step="0.001"
               class="forceStrengthSlider"
               v-model="linkStrength">
        <input type="number" min="0" max="0.2" step="0.001"
               class="forceStrengthNumber"
               v-model="linkStrength">
        Gravity strength
        <input type="range" min="0" max="800" step="1"
               class="forceStrengthSlider"
               v-model="gravityStrength">
        <input type="number" min="0" max="800" step="1"
               class="forceStrengthNumber"
               v-model="gravityStrength">
      </div>
      <div class="graph-editor-toolbar">
        <button v-on:click="autoLayout(); freezeAllNodes()">Auto-Layout and freeze</button>
        <button v-on:click="autoLayout">Auto-Layout</button>
        <button v-on:click="saveGraph">Save SVG</button>
        <button style="margin-right: auto" v-if="shouldShowSaveAPTButton"
                v-on:click="saveGraphAsAPT">
          Save graph as APT with X/Y coordinates
        </button>
        <button style="margin-left: auto" v-on:click="moveNodesToVisibleArea">
          Move all nodes into the visible area
        </button>
        <button style="display: none;" v-on:click="updateD3">Update D3</button>
        <button v-on:click="freezeAllNodes">Freeze all nodes</button>
        <button class="btn-danger" v-on:click="unfreezeAllNodes">Unfreeze all nodes</button>
      </div>
    </div>
    <div style="height:50px; display:none">
      <pre>{{ this.links }}</pre>
    </div>

    <svg class='graph' :id='this.graphSvgId' style="position: absolute; z-index: 0;">

    </svg>
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
    width: 100px;
  }
</style>

<script>
  import * as d3 from 'd3'
  import { saveFileAs } from '@/fileutilities'
  import { layoutNodes } from '@/autoLayout'
  import { pointOnRect, pointOnCircle } from '@/shapeIntersections'

  // Polyfill for IntersectionObserver API.  Used to detect whether graph is visible or not.
  require('intersection-observer')

  export default {
    name: 'graph-editor',
    components: {},
    mounted: function () {
      this.importGraph(this.graph)
      this.initializeD3()
      this.updateRepulsionStrength(this.repulsionStrength)
      this.updateLinkStrength(this.linkStrength)
      this.updateGravityStrength(this.gravityStrength)
      this.updateSvgDimensions()
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
      dragDrop: function () {
        // We save the start position of every dragdrop to tell if it is actually a mouse click.
        // This is necessary because otherwise, dragdrops and mouse clicks interfere with each other.
        // Either the dragdrop overrides the mouse click, forcing you to click perfectly,
        // or the dragdrop and mouse click fire the same event twice in a row.
        // So we won't bother listening for mouse clicks separately.  We just process mouse clicks
        // as a special case of dragdrop.  A dragdrop over a short distance counts as a mouse click.
        let xStart
        let yStart
        const deadzone = 20
        let deadzoneExceeded
        return d3.drag()
          .on('start', node => {
            node.fx = node.x
            node.fy = node.y
            xStart = d3.event.x
            yStart = d3.event.y
            deadzoneExceeded = false
          })
          .on('drag', node => {
            if (!deadzoneExceeded) {
              const distanceDragged = Math.sqrt(Math.pow(d3.event.x - xStart, 2) + Math.pow(d3.event.y - yStart, 2))
              if (distanceDragged > deadzone) {
                deadzoneExceeded = true
              }
            }
            if (deadzoneExceeded) {
              this.simulation.alphaTarget(0.7).restart()
              node.fx = d3.event.x
              node.fy = d3.event.y
            }
          })
          .on('end', node => {
            if (!d3.event.active) {
              this.simulation.alphaTarget(0)
            }
            this.onGraphModified()
            if (!deadzoneExceeded) {
              this.onNodeClick(node)
            }
          })
      }
    },
    watch: {
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
        saveGraphAPTRequestPreview: {},
        nodeRadius: 27,
        exportedGraphJson: {},
        nodes: this.deepCopy(this.graph.nodes),
        links: this.deepCopy(this.graph.links),
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
        gravityStrength: this.gravityStrengthDefault,
        onNodeClick: (d) => {
          // TODO Rename this from GRAPH_STRATEGY_BDD_STATE to BDD_GRAPH_STATE
          if (d.type === 'GRAPH_STRATEGY_BDD_STATE') {
            // Expand or collapse the postset of the State that has been clicked.
            // Freeze the State's position
            d.fx = d.x
            d.fy = d.y

            // Save the mouse coordinates so that the new nodes will appear where the user clicked.
            // I.e. at the location of the parent node that is being expanded.
            const mouseCoordinates = d3.mouse(this.svg.node())
            this.lastUserClick = {x: mouseCoordinates[0], y: mouseCoordinates[1]}
            // Toggle whether the postset of this State is visible
            this.$emit('toggleStatePostset', d.id)
          } else {
            // I like to be able to unfreeze nodes by clicking on them.
            d.fx = null
            d.fy = null
          }
        },
        onNodeRightClick: (d) => {
          d3.event.preventDefault() // Prevent the right click menu from appearing
          d3.event.stopPropagation()
          console.log(d)
          // TODO refactor duplicate code?
          if (d.type === 'GRAPH_STRATEGY_BDD_STATE') {
            // Freeze the State's position
            d.fx = d.x
            d.fy = d.y
            const mouseCoordinates = d3.mouse(this.svg.node())
            this.lastUserClick = {x: mouseCoordinates[0], y: mouseCoordinates[1]}
            // Toggle whether the preset of this State is visible
            this.$emit('toggleStatePreset', d.id)
          }
        }
      }
    },
    methods: {
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
        const centerX = this.dimensions.width / 2
        const centerY = this.dimensions.height / 2
        console.log(`Updating center force to coordinates: ${centerX}, ${centerY}`)
        // forceCenter is an alternative to forceX/forceY.  It works in a different way.  See D3's documentation.
        // this.simulation.force('center', d3.forceCenter(svgX / 2, svgY / 2))
        const centerStrength = 0.01
        this.simulation.force('centerX', d3.forceX(centerX).strength(centerStrength))
        this.simulation.force('centerY', d3.forceY(centerY).strength(centerStrength))
      },
      // TODO Run this whenever a new graph is loaded.  (But not upon changes to an existing graph)
      autoLayout: function () {
        const positionsPromise = layoutNodes(this.nodes, this.links, this.svgWidth(), this.svgHeight())
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
      // Sometimes nodes might get lost outside the borders of the screen.
      // This procedure places them back within the visible area.
      moveNodesToVisibleArea: function () {
        const margin = 45
        const boundingRect = this.svg.node().getBoundingClientRect()
        const maxX = boundingRect.width - margin
        const maxY = boundingRect.height - margin
        this.nodes.forEach(node => {
          const nodeIsFrozen = node.fx === node.x
          if (nodeIsFrozen) {
            if (node.x < margin) {
              node.fx = margin
            }
            if (node.y < margin) {
              node.fy = margin
            }
            if (node.x > maxX) {
              node.fx = maxX
            }
            if (node.y > maxY) {
              node.fy = maxY
            }
          } else {
            if (node.x < margin) {
              node.x = margin
            }
            if (node.y < margin) {
              node.y = margin
            }
            if (node.x > maxX) {
              node.x = maxX
            }
            if (node.y > maxY) {
              node.y = maxY
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
        console.log('GraphEditor: Emitting graphModified event: ')
        console.log(graph)
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

        this.linkGroup = this.svg.append('g').attr('class', 'links')
        this.linkTextGroup = this.svg.append('g').attr('class', 'linkTexts')
        this.nodeGroup = this.svg.append('g').attr('class', 'nodes')
        this.isSpecialGroup = this.svg.append('g').attr('class', 'isSpecialHighlights')
        this.labelGroup = this.svg.append('g').attr('class', 'texts')
        this.contentGroup = this.svg.append('g').attr('class', 'node-content')

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
          .call(this.dragDrop)
          .on('contextmenu', this.onNodeRightClick)
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
          .call(this.dragDrop)
          .on('contextmenu', this.onNodeRightClick)
          .attr('text-anchor', 'middle')
          // TODO Bug: The white-space attribute is not implemented for SVGs in Google Chrome.
          // TODO This means that our text will end up all on one line.  In Firefox it's ok, though.
          .style('white-space', 'pre')
        newContentElements.exit().remove()
        this.contentElements = contentEnter.merge(newContentElements)
        this.contentElements
          .attr('font-size', 15)
          .text(node => {
            if (node.type === 'GRAPH_STRATEGY_BDD_STATE') {
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
        newIsSpecialElements.call(this.dragDrop)
          .on('contextmenu', this.onNodeRightClick)
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
          .call(this.dragDrop)
          .on('contextmenu', this.onNodeRightClick)
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
            if (data.type === 'ENVPLACE') {
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
                // Do a bunch more fun math to make an arc
                const x1 = d.source.x
                const y1 = d.source.y
                const x2 = targetX
                const y2 = targetY
                const dx = x2 - x1
                const dy = y2 - y1
                const dr = Math.sqrt(dx * dx + dy * dy)
                // Defaults for normal edge.
                const drx = dr
                const dry = dr
                const xRotation = 0 // degrees
                const largeArc = 0 // 1 or 0
                const sweep = 1 // 1 or 0

                return 'M' + x1 + ',' + y1 + 'A' + drx + ',' + dry + ' ' + xRotation + ',' + largeArc + ',' + sweep + ' ' + x2 + ',' + y2
              } else if (linkIsLoop) {
                // Self edge.
                // Fiddle with this angle to get loop oriented.
                const xRotation = 0

                // Needs to be 1.
                const largeArc = 1
                // Change sweep to change orientation of loop.
                const sweep = 0

                // Make drx and dry different to get an ellipse
                // instead of a circle.
                const drx = 45
                const dry = 45

                // Place the loop around the upper-right corner of the node.
                const x1 = d.source.x + this.calculateNodeWidth(d.source) / 2
                const y1 = d.source.y
                const x2 = d.source.x
                const y2 = d.source.y - this.calculateNodeHeight(d.source) / 2
                return 'M' + x1 + ',' + y1 + 'A' + drx + ',' + dry + ' ' + xRotation + ',' + largeArc + ',' + sweep + ' ' + x2 + ',' + y2
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
          return 125 // TODO Make width expand to fit text (use fixed width font if necessary)
        } else {
          return this.nodeRadius * 2
        }
      },
      calculateNodeHeight: function (d) {
        if (d.content !== undefined) {
          return 90 // TODO Make height expand to fit text
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
