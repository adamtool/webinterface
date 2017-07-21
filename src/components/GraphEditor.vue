<template>
  <div id="graph-editor">
    <h2>Graph Editor</h2>
    parentGraph (A property given to us by our parent):
    <div>{{ parentGraph }}</div>
    graphDrawn (A data element that belongs to us):
    <div>{{ graphDrawn }}</div>
    Cytoscape graph:
    <div id='cy'></div>
  </div>
</template>

<script>
  //  import * as Vue from 'vue'
  import * as cytoscape from 'cytoscape'

  export default {
    name: 'graph-editor',
    components: {},
    data () {
      return {
        graphDrawn: this.parentGraph,
        cy: undefined
      }
    },
    methods: {
      convertGraphToCytoscapeElements: function (graph) {
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
      makeCytoscape: function (elements) {
        let cy = cytoscape({
          container: document.getElementById('cy'),
          elements: elements,
          style: [ // the stylesheet for the graph
            {
              selector: 'node',
              style: {
                'background-color': '#666',
                'label': 'data(id)'
              }
            },

            {
              selector: 'edge',
              style: {
                'width': 3,
                'line-color': '#ccc',
                'target-arrow-color': '#ccc',
                'target-arrow-shape': 'triangle'
              }
            }
          ],
          layout: {
            name: 'grid',
            rows: 1
          }
        })
        console.log('initialized cytoscape: ' + cy)
        return cy
      }
    },
    computed: {},
    props: ['parentGraph'],
    watch: {
      parentGraph: function (graph) {
        console.log('GraphEditor: parentGraph changed: ' + graph)
        /* When parentGraph changes, this should mean the user did one of the following things:
        1. Changed something in the APT editor, causing the APT to be parsed on the server, yielding a new graph

        In response, we will update the graph that is being edited in the drag-and-drop GUI of this component.
         */
        this.graphDrawn = graph
        // TODO: Figure out how to get decent debug output instead of a bunch of [object Object]
        let elements = this.convertGraphToCytoscapeElements(graph)
        console.log('Converted graph into cytoscapes format: ' + elements)
        this.cy = this.makeCytoscape(elements)
      }
    }
  }

</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
  #cy {
    width: 100%;
    height: 300px;
    display: block;
  }
</style>
