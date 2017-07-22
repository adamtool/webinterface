<template>
  <div id="graph-editor">
    <div class="row">
      <div class="col-12">
        <h2>Graph Editor</h2>
      </div>
    </div>
    <div class="row">
      <div class="col-12">
        Graph passed in from our parent:
        <div>{{ parentGraph }}</div>
      </div>
    </div>
    <!--<div class="row">-->
    <!--<div class="col-12">-->
    <!--exportedGraphJson-->
    <!--<div>{{ exportedGraphJson }}</div>-->
    <!--</div>-->
    <!--</div>-->
    <div class="row">
      <div class="col-12">
        apt of graph
        <div style="text-align: left">
          <pre><code>{{ graphApt }}</code></pre>
        </div>
      </div>
    </div>
    <div class="row">
      <div class="col-12">
        <button class="btn btn-primary" v-on:click="refreshCytoscape">Refresh Cytoscape</button>
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
        exportedGraphJson: {},
        cy: undefined
      }
    },
    mounted: function () {
      this.refreshCytoscape()
    },
    methods: {

      /**
       * Re-initialize our instance of Cytoscape using the graph given to us through the
       * 'parentGraph' prop.
       */
      refreshCytoscape: function () {
        let elements = this.convertCustomGraphJsonToCytoscape(this.parentGraph)
        // TODO: Figure out how to get decent debug output instead of a bunch of [object Object]
        console.log('Converted graph into cytoscapes format: ' + elements)
        this.cy = this.makeCytoscape(elements)
        this.exportedGraphJson = this.cy.json()
      },
      /**
       * Create a new instance of Cytoscape and embed it in the div #cy.
       * Set up all of our UI input event handlers.
       *
       * @param elements See http://js.cytoscape.org/#notation/elements-json
       */
      makeCytoscape: function (elements) {
        let cy = cytoscape({
          container: document.getElementById('cy'),
          elements: elements,
          style: [ // the stylesheet for the graph
            {
              selector: 'node',
              style: {
                'label': 'data(id)'
              }
            },
            {
              selector: ':locked',
              style: {
                'background-color': '#111'
              }
            },
            {
              selector: ':unlocked',
              style: {
                'background-color': '#777'
              }
            },
            {
              selector: 'edge',
              style: {
                'width': 3,
                'line-color': '#ccc',
                'curve-style': 'haystack',
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

        let self = this
        cy.nodes().on('click', function (event) {
          console.log(`Clicked node:${event.target.id()} with position x: ${event.target.position('x')}, y: ${event.target.position('y')}`)
          self.exportJson()
        })

        cy.nodes().on('free', function (event) {
          console.log(`Let go of node:${event.target.id()} with position x: ${event.target.position('x')}, y: ${event.target.position('y')}`)
          event.target.lock()
          console.log('locked node')
          self.exportJson()
        })
        console.log('Added free event handler')

        cy.nodes().on('grabon', function (event) {
          console.log('Grabbing node')
        })
        console.log('Added grabon event handler')

        cy.nodes().on('mousedown', function (event) {
          console.log('mousedown on node')
          event.target.unlock()
          console.log('unlocking node')
          self.exportJson()
        })
        console.log('Added mousedown event handler')
        return cy
      },
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
       * @param json
       */
      convertCytographJsonToApt: function (json) {
        if (!json.elements) {
          return ''
        }
        let nodes = json.elements.nodes
        let nodesApt = !nodes ? '' : nodes.map(node => {
          let coordinateString = node.locked ? `["x"="${node.position.x}", "y"="${node.position.y}"]` : ''
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
    },
    computed: {
      graphApt: function () {
        return this.convertCytographJsonToApt(this.exportedGraphJson)
      }
    },
    props: ['parentGraph'],
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
