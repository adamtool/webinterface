<template>
  <div id="graph-editor">
    <div class="row">
      <div class="col-12">
        <h2>Graph Editor</h2>
      </div>
    </div>
    <div class="row">
      <div class="col-12">
        ourGraph
        <div>{{ ourGraph }}</div>
      </div>
    </div>
    <div class="row">
      <div class="col-12">
        <button v-on:click="refreshCytoscape">Refresh Cytoscape</button>
        <div>Cytoscape graph:</div>
        <div id='cy'></div>
      </div>
    </div>
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
        ourGraph: this.parentGraph,
        cy: undefined
      }
    },
    mounted: function () {
      this.refreshCytoscape()
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
      refreshCytoscape: function () {
        let elements = this.convertGraphToCytoscapeElements(this.parentGraph)
        // TODO: Figure out how to get decent debug output instead of a bunch of [object Object]
        console.log('Converted graph into cytoscapes format: ' + elements)
        this.cy = this.makeCytoscape(elements)
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
            rows: 2
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
        this.ourGraph = graph
        this.refreshCytoscape()
      }
    }
  }

</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
  #cy {
    text-align: left;
    width: 100%;
    height: 300px;
    /*display: flex;*/
    /*position: absolute;*/
    /*top: 0px;*/
    /*left: 0px;*/
  }
</style>
