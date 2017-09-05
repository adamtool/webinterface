<template>
  <div id="graph-editor">
    <div id='overlay'>
      <div class="row">
        <div class="col-12">
          <h2>Graph Editor</h2>
        </div>
      </div>
      <div class="row">
        <div class="col-12">
          Graph passed in from our parent:
          <div>{{ parentGraph }}</div>
          <!--Our nodes:-->
          <!--<div>{{ nodes }}</div>-->
          <!--Our links:-->
          <!--<div>{{ links }}</div>-->
        </div>
      </div>
      <div class="row">
        <div class="col-12">
          <button class="btn btn-primary" v-on:click="refreshD3">Refresh Graph</button>
          <div>Cytoscape graph:</div>
        </div>
      </div>
    </div>
    <svg id='graph'>

    </svg>
  </div>
</template>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style>
  #graph-editor {
    height: 95vh;
    display: flex;
    flex-direction: column;
  }

  #graph {
    flex-grow: 1;
    text-align: left;
  }
</style>

<script>
  import * as d3 from 'd3'

  export default {
    name: 'graph-editor',
    components: {},
    mounted: function () {
      this.initializeD3()
    },
    props: ['parentGraph'],
    computed: {
      graphApt: function () {
        return this.convertCytographJsonToApt(this.exportedGraphJson)
      }
    },
    watch: {
      graphApt: function (apt) {
        console.log('Emitting graphModified event: ' + apt)
        this.$emit('graphModified', apt)
      },
      parentGraph: function (graph) {
        console.log('GraphEditor: parentGraph changed: ' + graph)
        /* When parentGraph changes, this most likely means that the user changed something in the
         APT editor, causing the APT to be parsed on the server, yielding a new graph.
         And then they hit the button "Send Graph to Editor".

         This would also be fired if the 'parentGraph' prop changed in response to any other
         events, such as after "Load"ing a saved graph in the main App's UI.

         In response, we will update the graph that is being edited in the drag-and-drop GUI of
         this component.
         */
        const convertedGraph = this.convertCustomGraphJsonToD3(graph)
        this.nodes = convertedGraph.nodes
        this.links = convertedGraph.links
        this.refreshD3()
      }
    },
    data () {
      return {
        exportedGraphJson: {},
        nodes: [
          {id: 'mammal', group: 0, label: 'Mammals', level: 1},
          {id: 'dog', group: 0, label: 'Dogs', level: 2},
          {id: 'cat', group: 0, label: 'Cats', level: 2},
          {id: 'fox', group: 0, label: 'Foxes', level: 2},
          {id: 'elk', group: 0, label: 'Elk', level: 2},
          {id: 'insect', group: 1, label: 'Insects', level: 1},
          {id: 'ant', group: 1, label: 'Ants', level: 2},
          {id: 'bee', group: 1, label: 'Bees', level: 2},
          {id: 'fish', group: 2, label: 'Fish', level: 1},
          {id: 'carp', group: 2, label: 'Carp', level: 2},
          {id: 'pike', group: 2, label: 'Pikes', level: 2}
        ],
        links: [
          {target: 'mammal', source: 'dog', strength: 0.7},
          {target: 'mammal', source: 'cat', strength: 0.7},
          {target: 'mammal', source: 'fox', strength: 0.7},
          {target: 'mammal', source: 'elk', strength: 0.7},
          {target: 'insect', source: 'ant', strength: 0.7},
          {target: 'insect', source: 'bee', strength: 0.7},
          {target: 'fish', source: 'carp', strength: 0.7},
          {target: 'fish', source: 'pike', strength: 0.7},

          {target: 'cat', source: 'elk', strength: 0.1},
          {target: 'carp', source: 'ant', strength: 0.1},
          {target: 'elk', source: 'bee', strength: 0.1},
          {target: 'dog', source: 'cat', strength: 0.1},
          {target: 'fox', source: 'ant', strength: 0.1},
          {target: 'pike', source: 'dog', strength: 0.1}
        ],
        svg: undefined,
        linkGroup: undefined,
        nodeGroup: undefined,
        textGroup: undefined,
        nodeElements: undefined,
        linkElements: undefined,
        textElements: undefined,
        simulation: d3.forceSimulation()
          .force('gravity', d3.forceManyBody().strength(100).distanceMin(450))
          .force('charge', d3.forceManyBody().strength(-50))
          //          .force('center', d3.forceCenter(width / 2, height / 2))
          .force('link', d3.forceLink()
            .id(link => link.id)
            .strength(0.1))
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
            node.fx = null
            node.fy = null
          })
      }
    },
    methods: {
      initializeD3: function () {
        this.svg = d3.select('#graph')
          .attr('width', '100%')
          .attr('height', '100%')

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
          this.refreshD3()
        }
        const insertNodeOnClick = () => {
          const coordinates = d3.mouse(this.svg.node())
          console.log('Click event registered.  Coordinates:')
          console.log(coordinates)
          const label = Math.random().toString()
          insertNode(label, label, coordinates[0], coordinates[1])
        }

        this.svg.on('click', insertNodeOnClick)

        console.log(this.svg.node())
        const updateCenterForce = () => {
          console.log('Updating center force')
          const svgWidth = this.svg.node().clientWidth
          const svgHeight = this.svg.node().clientHeight
          // forceCenter is an alternative to forceX/forceY.  It works in a different way.  See D3's documentation.
          // this.simulation.force('center', d3.forceCenter(svgX / 2, svgY / 2))
          const centerStrength = 0.01
          this.simulation.force('centerX', d3.forceX(svgWidth / 2).strength(centerStrength))
          this.simulation.force('centerY', d3.forceY(svgHeight / 2).strength(centerStrength))
        }
        window.addEventListener('resize', updateCenterForce)
        updateCenterForce()

        this.refreshD3()

//        d3.selectAll('*').on('click', function (d) { console.log(d) })
      },
      refreshD3: function () {
        const newNodeElements = this.nodeGroup
          .selectAll('circle')
          .data(this.nodes, node => node.id)
        const nodeEnter = newNodeElements
          .enter().append('circle')
          .attr('r', 20)
          .attr('fill', node => node.level === 1 ? 'red' : 'gray')
          .call(this.dragDrop)
        newNodeElements.exit().remove()
        this.nodeElements = nodeEnter.merge(newNodeElements)

        const newTextElements = this.textGroup
          .selectAll('text')
          .data(this.nodes, node => node.id)
        const textEnter = newTextElements
          .enter().append('text')
          .text(node => node.label)
          .attr('font-size', 15)
          .attr('dx', 15)
          .attr('dy', 4)
        newTextElements.exit().remove()
        this.textElements = textEnter.merge(newTextElements)

        const newLinkElements = this.linkGroup
          .selectAll('line')
          .data(this.links)
        const linkEnter = newLinkElements
          .enter().append('line')
          .attr('stroke-width', 3)
          .attr('stroke', '#E5E5E5')
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
      // Export graph as Json.  TODO eliminate this step of the process.  (It's unnecessary.)
      exportJson: function () {
        this.exportedGraphJson = this.cy.json()
      },
      /**
       * Convert my own custom graph JSON format into D3's own JSON format.
       * TODO: Use x and y coordinates provided in JSON.  Consider just using D3's format to begin with.
       *
       */
      convertCustomGraphJsonToD3: function (graph) {
        let nodes = []
        for (let i = 0; i < graph.nodes.length; i++) {
          let node = graph.nodes[i]
          console.log('converting node:' + node)
          nodes.push({
            id: node.toString(),
            label: node.toString(),
            group: 0,
            level: 0
          })
        }

        let links = []
        console.log('converting edges: ' + graph.edges)
        for (let i = 0; i < graph.edges.length; i++) {
          let edge = graph.edges[i]
          console.log('converting edge: ' + edge)
          links.push({
            source: edge[0].toString(),
            target: edge[1].toString(),
            strength: 0.1
          })
        }
        return {
          nodes: nodes,
          links: links
        }
      },
      /**
       * Convert Cytograph's JSON format into APT
       * // TODO skip the step of JSON export.  Just export the data structure directly
       * @param json
       */
      convertCytographJsonToApt: function (json) {
        if (!json.elements) {
          return ''
        }
        let nodes = json.elements.nodes
        let nodesApt = !nodes ? '' : nodes.map(node => {
          let coordinateString = `["x"="${node.position.x.toFixed(0)}", "y"="${node.position.y.toFixed(0)}, "fixed"="${node.data.fixedByUser ? 'true' : 'false'}"]`
          let nodeRepresentation = node.data.id + coordinateString
          return nodeRepresentation
        }).join('\n')
        let edges = json.elements.edges
        let edgesApt = !edges ? '' : edges.map(edge => {
          let edgeString = `${edge.data.source} -> ${edge.data.target}`
          return edgeString
        }).join('\n')
        return `# Nodes\n${nodesApt}\n\n# Edges\n${edgesApt}`
      }
    }
  }

</script>
