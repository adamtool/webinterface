<template>
  <div class="graph-editor">
    <div class="graph-editor-toolbar">
      Repulsion Strength
      <input type="range" min="30" max="500" step="1"
             class="repulsionStrengthSlider"
             v-model="repulsionStrength">
      <input type="number" min="30" max="500" step="1"
             class="repulsionStrengthNumber"
             v-model="repulsionStrength">
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

  .repulsionStrengthSlider {
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
    },
    props: ['petriNet'],
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
        this.simulation.force('charge').strength(-strength)
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
        // TODO Bug fix: Right now, the graph warps up to the upper-left corner of the screen
        // TODO every time you change it.
        // TODO This is a result of the fact that we completely throw out our existing nodes
        // TODO array, which contains the x/y coordinates of every node.
        // TODO To fix the bug, we have to diff the new set of nodes against our existing nodes
        // TODO and add the new nodes into our existing array (and as a refinement, give them
        // TODO appropriate x/y coordinates near to the x/y coordinates of their parent nodes
        this.importGraph(graph)
        this.updateD3()
      }
    },
    data () {
      return {
        nodeSize: 50,
        repulsionStrength: 120,
        exportedGraphJson: {},
        nodes: this.deepCopy(this.petriNet.nodes),
        links: this.deepCopy(this.petriNet.links),
        svg: undefined,
        linkGroup: undefined,
        nodeGroup: undefined,
        textGroup: undefined,
        nodeElements: undefined,
        linkElements: undefined,
        textElements: undefined,
        simulation: d3.forceSimulation()
          .force('gravity', d3.forceManyBody().strength(100).distanceMin(1000))
          .force('charge', d3.forceManyBody().strength(-120))
          //          .force('center', d3.forceCenter(width / 2, height / 2))
          .force('link', d3.forceLink()
            .id(link => link.id)
            .strength(0.05))
          .alphaMin(0.002),
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
            // Toggle the state of the node (hiding/showing its postset) (assuming the event is appropriately bound in our parent)
            this.$emit('expandOrCollapseState', d.id)
          }
        },
        onNodeRightClick: (d) => {
          d3.event.preventDefault() // Prevent the right click menu from appearing
          d3.event.stopPropagation()
          if (d.type === 'GRAPH_STRATEGY_BDD_STATE') {
            d.shouldShowContent = (d.content !== null) && !d.shouldShowContent
            console.log(`shouldShowContent: ${d.shouldShowContent}`)
            this.updateD3()
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
          .attr('refX', this.nodeSize / 2)
          .attr('refY', -1.5)
          .attr('markerWidth', 6)
          .attr('markerHeight', 6)
          .attr('orient', 'auto')
          .append('svg:path')
          .attr('d', 'M0,-5L10,0L0,5')

        this.linkGroup = this.svg.append('g').attr('class', 'links')
        this.nodeGroup = this.svg.append('g').attr('class', 'nodes')
        this.textGroup = this.svg.append('g').attr('class', 'texts')

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
          .attr('width', this.nodeSize)
          .attr('height', this.nodeSize)
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
              return 'white'
            } else {
              return 'black' // TODO Throw some kind of exception or error.  This should be an exhaustive pattern match
            }
          })

        const newTextElements = this.textGroup
          .selectAll('text')
          .data(this.nodes, this.keyFunction)
        const textEnter = newTextElements
          .enter().append('text')
          .call(this.dragDrop)
          .on('click', this.onNodeClick)
          .on('contextmenu', this.onNodeRightClick)
        newTextElements.exit().remove()
        this.textElements = textEnter.merge(newTextElements)
        this.textElements
          .attr('font-size', 15)
          .text(node => {
            return node.shouldShowContent ? node.content : node.label
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
            .attr('transform', node => `translate(${node.x - this.nodeSize / 2},${node.y - this.nodeSize / 2})`)
          this.nodeElements.filter('circle')
            .attr('transform', node => `translate(${node.x},${node.y})`)
          this.textElements
            .attr('x', node => node.x - this.nodeSize / 2 + 5)
            .attr('y', node => node.y + 3)
          this.linkElements
            .attr('x1', link => link.source.x)
            .attr('y1', link => link.source.y)
            .attr('x2', link => link.target.x)
            .attr('y2', link => link.target.y)

          // Let the simulation know what links it is working with
          this.simulation.force('link').links(this.links)

          // Raise the temperature of the force simulation, because otherwise, if the temperature is below alphaMin, the newly
          // inserted nodes' positions will not get updated, and they will appear in the upper left corner of the svg
          // until something causes the temperature to increase again past the threshold.
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
            this.links.push(newLink)
          }
        })

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
            // TODO Place new nodes next to their parents, or next to the last place the user clicked or something
            newNode.x = this.svgWidth() / 2
            newNode.y = this.svgHeight() / 2
            this.nodes.push(newNode)
          }
        })
      },
      keyFunction: function (data) {
        return `${data.id}::${data.type}`
      },
      svgWidth: function () {
        return this.svg.node().getBoundingClientRect().width
      },
      svgHeight: function () {
        return this.svg.node().getBoundingClientRect().height
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
