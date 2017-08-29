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
          <button class="btn btn-primary" v-on:click="initializeD3">Refresh Graph</button>
          <div>Cytoscape graph:</div>
        </div>
      </div>
    </div>
    <div id='graph'>

    </div>
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
        ]
      }
    },
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
        this.initializeD3()
      }
    },
    methods: {
      initializeD3: function () {
//        const groups = {
//          0: 'mammal',
//          1: 'insect',
//          2: 'fish'
//        }
        const nodes = this.nodes
        const links = this.links

        const width = window.innerWidth
        const height = window.innerHeight

        const svg = d3.select('#graph')
          .append('svg')
          .attr('width', width)
          .attr('height', height)

        const linkForce = d3.forceLink()
          .id(link => link.id)
          .strength(link => link.strength)

        const simulation = d3.forceSimulation()
          .force('charge', d3.forceManyBody().strength(-120))
          .force('center', d3.forceCenter(width / 2, height / 2))
          .force('link', linkForce)

        function getNodeColor (node) {
          return node.level === 1 ? 'red' : 'gray'
        }

        const nodeElements = svg.append('g')
          .selectAll('circle')
          .data(nodes)
          .enter().append('circle')
          .attr('r', 10)
          .attr('fill', getNodeColor)

        const textElements = svg.append('g')
          .selectAll('text')
          .data(nodes)
          .enter().append('text')
          .text(node => node.label)
          .attr('font-size', 15)
          .attr('dx', 15)
          .attr('dy', 4)

        simulation.nodes(nodes).on('tick', () => {
          nodeElements
            .attr('cx', node => node.x)
            .attr('cy', node => node.y)
          textElements
            .attr('x', node => node.x)
            .attr('y', node => node.y)
          linkElements
            .attr('x1', link => link.source.x)
            .attr('y1', link => link.source.y)
            .attr('x2', link => link.target.x)
            .attr('y2', link => link.target.y)
        })

        simulation.force('link').links(links)

        const linkElements = svg.append('g')
          .selectAll('line')
          .data(links)
          .enter().append('line')
          .attr('stroke-width', 5)
          .attr('stroke', '#E5E5E5')

        d3.selectAll('*').on('click', function (d) { console.log(d) })
      },
      // Export graph as Json.  TODO eliminate this step of the process.  (It's unnecessary.)
      exportJson: function () {
        this.exportedGraphJson = this.cy.json()
      },
      /**
       * Convert my own custom graph JSON format into Cytoscape's own JSON format.
       * TODO: Support X and Y coordinates.  Consider just using Cytoscape's format to begin with.
       *
       */
      convertCustomGraphJsonToCytoscape: function (graph) {
        let elements = []
        for (let i = 0; i < graph.nodes.length; i++) {
          let node = graph.nodes[i]
          console.log('converting node:' + node)
          elements.push({
            data: {id: node.toString()}
          })
        }
        console.log('converting edges: ' + graph.edges)
        for (let i = 0; i < graph.edges.length; i++) {
          let edge = graph.edges[i]
          console.log('converting edge: ' + edge)
          elements.push({
            data: {
              id: edge[0].toString() + '->' + edge[1].toString(),
              source: edge[0].toString(),
              target: edge[1].toString()
            }
          })
        }
        return elements
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
