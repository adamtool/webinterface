<template>
  <div class="graph-editor">
    <svg class='graph' v-bind:id='this.graphSvgId()'>

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
      prettyGraphJson: function () {
        return JSON.stringify(this.nodes, null, '\t')
      }
    },
    watch: {
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
        const convertedGraph = this.importGraph(graph)
        this.nodes = convertedGraph.nodes
        this.links = convertedGraph.links
        this.updateD3()
      }
    },
    data () {
      return {
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
          .force('charge', d3.forceManyBody().strength(-80))
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
          })
      }
    },
    methods: {
      graphSvgId: function () {
        return 'graph-' + this._uid
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
        this.svg = d3.select('#' + this.graphSvgId())
          .attr('width', '100%')
          .attr('height', '100%')

        // Define arrows
        this.svg.append('svg:defs').selectAll('marker')
          .data(['end'])      // Different link/path types can be defined here
          .enter().append('svg:marker')    // This section adds in the arrows
          .attr('id', 'arrowhead')
          .attr('viewBox', '0 -5 10 10')
          .attr('refX', 15)
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

        const insertNode = (id, label, x, y) => {
          this.nodes.push({
            id: id,
            label: label,
            x: x,
            y: y
          })
          this.updateD3()
          this.onGraphModified()
        }
        const insertNodeOnClick = () => {
          const coordinates = d3.mouse(this.svg.node())
          console.log('Click event registered.  Coordinates:')
          console.log(coordinates)
          const label = Math.random().toString()
          insertNode(label, label, coordinates[0], coordinates[1])
        }

        this.svg.on('click', insertNodeOnClick)

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

        // We update the center force when the viewing area becomes visible, because until that point, there is no way of telling
        // where the center of the viewing area is, so our center force ends up putting the graph in the upper-left
        // corner of the screen.
        // There is probably a way of setting things up so that the graph will just start in the center of the viewing area,
        // but I'm not sure yet what the best way to do that would be.  This gets the job done for now.

        // See https://developer.mozilla.org/en-US/docs/Web/API/Intersection_Observer_API
        const onGraphVisibilityChange = (entries, observer) => {
          entries.forEach(function (entry) {
            if (entry.isIntersecting) {
              updateCenterForce()
            }
          })
        }
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
        const newNodeElements = this.nodeGroup
          .selectAll('circle')
          .data(this.nodes, node => node.id)
        const nodeEnter = newNodeElements
          .enter().append('circle')
          .attr('r', 20)
          .attr('fill', node => {
            if (node.type === 'ENVPLACE') {
              return 'tomato'
            } else if (node.type === 'SYSPLACE') {
              return 'skyblue'
            } else if (node.type === 'TRANSITION') {
              return 'grey'
            } else {
              return 'black' // TODO Throw some kind of exception or error.  This should be an exhaustive pattern match
            }
          })
          .call(this.dragDrop)
          .on('click', (d) => {
            d3.event.stopPropagation() // Do this, or else a new node will be placed underneath the node you clicked
            d.fx = null
            d.fy = null
            this.onGraphModified()
          })
        newNodeElements.exit().remove()
        this.nodeElements = nodeEnter.merge(newNodeElements)

        const newTextElements = this.textGroup
          .selectAll('text')
          .data(this.nodes, node => node.id)
        const textEnter = newTextElements
          .enter().append('text')
          .text(node => node.label)
          .attr('font-size', 15)
          .attr('dx', 25)
          .attr('dy', 4)
          .call(this.dragDrop)
          .on('click', (d) => {
            d3.event.stopPropagation()
            d.fx = null
            d.fy = null
          })
        newTextElements.exit().remove()
        this.textElements = textEnter.merge(newTextElements)

        const newLinkElements = this.linkGroup
          .selectAll('line')
          .data(this.links)
        const linkEnter = newLinkElements
          .enter().append('line')
          .attr('stroke-width', 3)
          .attr('stroke', '#E5E5E5')
          .attr('marker-end', 'url(#arrowhead)')
        newLinkElements.exit().remove()
        this.linkElements = linkEnter.merge(newLinkElements)

        this.updateSimulation()
      },
      updateSimulation: function () {
        this.simulation.nodes(this.nodes).on('tick', () => {
          this.nodeElements
            .attr('cx', node => node.x)
            .attr('cy', node => node.y)
          this.textElements
            .attr('x', node => node.x)
            .attr('y', node => node.y)
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
       * Perform a defensive copy of the graph JSON given to us from the backend.
       * TODO Validate it -- make sure it has all the properties we expect to see.
       */
      importGraph: function (graphJson) {
        const graphJsonCopy = this.deepCopy(graphJson)
        return graphJsonCopy
//        We should put up some kind of error message if any of the expected properties aren't there.
//        return {
//          links: graphJsonCopy.links.map(link => ({
//            source: link.source,
//            target: link.target
//          })),
//          nodes: graphJsonCopy.nodes.map(node => ({
//            id: node.id,
//            label: node.label,
//            type: node.type,
//            x: this.svgWidth() / 2,
//            y: this.svgHeight() / 2,
//            group: 0,
//            level: 0
//          }))
//        }
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
