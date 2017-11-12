<template>
  <div class="graph-editor">
    <div class="graph-editor-toolbar" v-if="shouldShowPhysicsControls">
      Repulsion Strength
      <input type="range" min="30" max="500" step="1"
             class="forceStrengthSlider"
             v-model="repulsionStrength">
      <input type="number" min="30" max="500" step="1"
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
      <button v-on:click="simulation.stop()">Stop simulation</button>
      <button v-on:click="simulation.restart()">Restart simulation</button>
      <button v-on:click="updateD3()">Update D3</button>
    </div>
    <svg class='graph' v-bind:id='this.graphSvgId'>

    </svg>
  </div>
</template>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
  .graph-editor {
    height: 95vh;
    display: flex;
    flex-direction: column;
  }

  .graph {
    flex-grow: 1;
    text-align: left;
  }

  .graph-editor-toolbar {
    display: flex;
    flex-direction: row;
  }

  .forceStrengthSlider {
    flex-grow: 0.5;
  }
</style>

<script>
  import * as d3 from 'd3'
  // Polyfill for IntersectionObserver API.  Used to detect whether graph is visible or not.
  require('intersection-observer')

  export default {
    name: 'graph-editor',
    components: {},
    mounted: function () {
      this.initializeD3()
      this.updateRepulsionStrength(this.repulsionStrength)
      this.updateLinkStrength(this.linkStrength)
      this.updateGravityStrength(this.gravityStrength)
    },
    props: {
      petriNet: {
        type: Object,
        required: true
      },
      shouldShowPhysicsControls: {
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
      graphSvgId: function () {
        return 'graph-' + this._uid
      },
      arrowheadId: function () {
        return 'arrowhead-' + this._uid
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
      petriNet: function (graph) {
        console.log('GraphEditor: petriNet changed:')
        console.log(graph)
        /* When petriNet changes, this most likely means that the user changed something in the
         APT editor, causing the APT to be parsed on the server, yielding a new graph.
         And then they hit the button "Send Graph to Editor".

         This would also be fired if the 'petriNet' prop changed in response to any other
         events, such as after "Load"ing a saved graph in the main App's UI.

         In response, we will update the graph that is being edited in the drag-and-drop GUI of
         this component.
         */
        this.importGraph(graph)
        this.updateD3()
      }
    },
    data () {
      return {
        nodeSize: 50,
        exportedGraphJson: {},
        nodes: this.deepCopy(this.petriNet.nodes),
        links: this.deepCopy(this.petriNet.links),
        svg: undefined,
        linkGroup: undefined,
        nodeGroup: undefined,
        labelGroup: undefined,
        contentGroup: undefined,
        nodeElements: undefined,
        linkElements: undefined,
        labelElements: undefined,
        contentElements: undefined,
        nodeSpawnPoint: {x: 0, y: 0},
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
        dragDrop: d3.drag()
          .on('start', node => {
            node.fx = node.x
            node.fy = node.y
          })
          .on('drag', node => {
            this.simulation.alphaTarget(0.7).restart()
            node.fx = d3.event.x
            node.fy = d3.event.y
          })
          .on('end', node => {
            if (!d3.event.active) {
              this.simulation.alphaTarget(0)
            }
            this.onGraphModified()
          }),
        onNodeClick: (d) => {
          d3.event.stopPropagation() // Prevent the click event from reaching all the other elements that overlap the node.
          d.fx = null
          d.fy = null
          this.onGraphModified()
          // TODO Rename this from GRAPH_STRATEGY_BDD_STATE to BDD_GRAPH_STATE
          if (d.type === 'GRAPH_STRATEGY_BDD_STATE') {
            // Save the mouse coordinates so that the new nodes will appear where the user clicked.
            // In practice, this will be inside of the parent node that is being expanded, which is
            // what we want, but there may be edge cases where e.g. the user clicks on two nodes
            // very quickly to expand them both, and the children of both parent nodes may appear
            // at the location of the second parent.
            const mouseCoordinates = d3.mouse(this.svg.node())
            this.nodeSpawnPoint = {x: mouseCoordinates[0], y: mouseCoordinates[1]}
            // Toggle the state of the node (hiding/showing its postset) (assuming the event is appropriately bound in our parent)
            this.$emit('expandOrCollapseState', d.id)
          }
        },
        onNodeRightClick: (d) => {
          d3.event.preventDefault() // Prevent the right click menu from appearing
          d3.event.stopPropagation()
          console.log(d)
          // TODO refactor duplicate code?
          if (d.type === 'GRAPH_STRATEGY_BDD_STATE') {
            const mouseCoordinates = d3.mouse(this.svg.node())
            this.nodeSpawnPoint = {x: mouseCoordinates[0], y: mouseCoordinates[1]}
            // Toggle the state of the node (hiding/showing its postset) (assuming the event is appropriately bound in our parent)
            this.$emit('expandOrCollapseState', d.id)
          }
        }
      }
    },
    methods: {
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
          .attr('width', '100%')
          .attr('height', '100%')

        // Prevent the right click menu from showing up.
        this.svg.on('contextmenu', () => {
          d3.event.preventDefault()
        })

        // Define arrows
        this.svg.append('svg:defs').selectAll('marker')
          .data(['end'])      // Different link/path types can be defined here
          .enter().append('svg:marker')    // This section adds in the arrows
          .attr('id', this.arrowheadId)
          .attr('viewBox', '0 -5 10 10')
          // TODO Make arrowheads work also for nodes of variable sizes
          .attr('refX', this.nodeSize / 2)
          .attr('refY', -1.5)
          .attr('markerWidth', 6)
          .attr('markerHeight', 6)
          .attr('orient', 'auto')
          .append('svg:path')
          .attr('d', 'M0,-5L10,0L0,5')

        this.linkGroup = this.svg.append('g').attr('class', 'links')
        this.nodeGroup = this.svg.append('g').attr('class', 'nodes')
        this.labelGroup = this.svg.append('g').attr('class', 'texts')
        this.contentGroup = this.svg.append('g').attr('class', 'node-content')

        console.log('force simulation minimum alpha value: ' + this.simulation.alphaMin())

        /**
         * We try to keep the Petri Net centered in the middle of the viewing area by applying a force to it.
         */
        const updateCenterForce = () => {
          console.log('Updating center force')
          // forceCenter is an alternative to forceX/forceY.  It works in a different way.  See D3's documentation.
          // this.simulation.force('center', d3.forceCenter(svgX / 2, svgY / 2))
          const centerStrength = 0.01
          this.simulation.force('centerX', d3.forceX(this.svgWidth() / 2).strength(centerStrength))
          this.simulation.force('centerY', d3.forceY(this.svgHeight() / 2).strength(centerStrength))
        }
        window.addEventListener('resize', updateCenterForce)

        // HACK HACK HACK HACK HACK
        // We update the center force when the viewing area becomes visible, because until that point, there is no way of telling
        // where the center of the viewing area is from within this component.
        // Our center force ends up putting the graph in the upper-left corner of the screen.
        // TODO replace this workaround with a better solution.  E.g. Specify the center of the SVG as a property
        // of this component.
        // TODO note that this workaround/hack seems to hurt our performance.
        const onGraphVisibilityChange = (entries, observer) => {
          entries.forEach(function (entry) {
            if (entry.isIntersecting) {
              console.log('Updating center force')
              updateCenterForce()
            }
          })
        }
        // See https://developer.mozilla.org/en-US/docs/Web/API/Intersection_Observer_API for info on how this works.
        const observer = new IntersectionObserver(onGraphVisibilityChange, {
          threshold: 0.01
        })
        observer.observe(this.svg.node())
        updateCenterForce()

        this.updateD3()

//        d3.selectAll('*').on('click', function (d) { console.log(d) })
      },
      /**
       * This method should be called every time the "nodes" or "links" arrays are updated.
       * It causes our visualization to update accordingly, showing new nodes and removing deleted ones.
       */
      updateD3: function () {
        const newLabelElements = this.labelGroup
          .selectAll('text')
          .data(this.nodes, this.keyFunction)
        const labelEnter = newLabelElements
          .enter().append('text')
          .call(this.dragDrop)
          .on('click', this.onNodeClick)
          .on('contextmenu', this.onNodeRightClick)
          .attr('text-anchor', 'middle')
        newLabelElements.exit().remove()
        this.labelElements = labelEnter.merge(newLabelElements)
        this.labelElements
          .attr('font-size', 15)
          .text(node => node.label)

        const newContentElements = this.contentGroup
          .selectAll('text')
          .data(this.nodes.filter(node => node.content !== undefined), this.keyFunction)
        const contentEnter = newContentElements
          .enter().append('text')
          .call(this.dragDrop)
          .on('click', this.onNodeClick)
          .on('contextmenu', this.onNodeRightClick)
          .attr('text-anchor', 'middle')
          // TODO Bug: The white-space attribute is not implemented for SVGs in Google Chrome.
          // TODO This means that our text will end up all on one line.  In Firefox it's ok, though.
          .style('white-space', 'pre')
        newContentElements.exit().remove()
        this.contentElements = contentEnter.merge(newContentElements)
        this.contentElements
          .attr('font-size', 15)
          .text(node => node.content)

        const nodeElements = this.nodeGroup
          .selectAll('.graph-node')
          .data(this.nodes, this.keyFunction)
        const newNodeElements = nodeElements.enter().append((node) => {
          const shape = (node.type === 'ENVPLACE' || node.type === 'SYSPLACE') ? 'circle' : 'rect'
          return document.createElementNS('http://www.w3.org/2000/svg', shape)
        })
        newNodeElements
          .call(this.dragDrop)
          .on('click', this.onNodeClick)
          .on('contextmenu', this.onNodeRightClick)
        nodeElements.exit().remove()
        this.nodeElements = nodeElements.merge(newNodeElements)
        this.nodeElements
          .attr('class', d => `graph-node ${d.type}`)
          .attr('r', this.nodeSize / 1.85)
          .attr('width', this.calculateNodeWidth)
          .attr('height', this.calculateNodeHeight)
          .attr('stroke', 'black')
          .attr('stroke-width', 2)
          .attr('fill', data => {
            if (data.type === 'ENVPLACE') {
              return 'white'
            } else if (data.type === 'SYSPLACE') {
              return 'lightgrey'
            } else if (data.type === 'TRANSITION') {
              return 'white'
            } else if (data.type === 'GRAPH_STRATEGY_BDD_STATE') {
              return data.isExpanded ? 'white' : 'lightgrey' // TODO Find out from Manuel how he thinks this should be depicted
              // TODO Consider adding some visual indicator of whether a node has a Postset that includes other nodes
              // TODO Consider showing a preview of a node's postset upon mouseover
            } else {
              return 'black' // TODO Throw some kind of exception or error.  This should be an exhaustive pattern match
            }
          })

        const newLinkElements = this.linkGroup
          .selectAll('line')
          .data(this.links)
        const linkEnter = newLinkElements
          .enter().append('line')
        newLinkElements.exit().remove()
        this.linkElements = linkEnter.merge(newLinkElements)
          .attr('stroke-width', 3)
          .attr('stroke', '#E5E5E5')
          .attr('marker-end', 'url(#' + this.arrowheadId + ')')

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
          this.labelElements
            .attr('x', node => node.x)
            // TODO Use function mentioned in other TODO to determine where the bottom of the node is
            .attr('y', node => node.y + this.calculateNodeHeight(node) / 2 + 15)
          this.contentElements
            .attr('x', node => node.x)
            .attr('y', node => node.y - this.calculateNodeHeight(node) / 2 + 30)
          this.linkElements
            .attr('x1', link => link.source.x)
            .attr('y1', link => link.source.y)
            .attr('x2', link => link.target.x)
            .attr('y2', link => link.target.y)

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
            // TODO Consider fixing nodes for which "isExpanded" is true.  Right now, nodes tend to
            // get pushed around in a disorienting way when their children are added to the graph.
            // Randomize the position slightly to stop the nodes from flying away from each other
            newNode.x = this.nodeSpawnPoint.x + (Math.random() - 0.5) * 40
            newNode.y = this.nodeSpawnPoint.y + (Math.random() - 0.5) * 40
            this.nodes.push(newNode)
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
            const newLinkWithReferences = {
              source: this.nodes.find(node => node.id === newLink.source),
              target: this.nodes.find(node => node.id === newLink.target)
            }
            this.links.push(newLinkWithReferences)
          }
        })
      },
      keyFunction: function (data) {
        return `${data.id}::${data.type}`
      },
      calculateNodeWidth: function (d) {
        if (d.content !== undefined) {
          return 125
        } else {
          return this.nodeSize
        }
      },
      calculateNodeHeight: function (d) {
        if (d.content !== undefined) {
          return 90
        } else {
          return this.nodeSize
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
      }
    }
  }

</script>
