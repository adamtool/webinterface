<template>
  <div id="graph-editor">
    <div id='cy'></div>
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
        </div>
      </div>
      <!--<div class="row">-->
      <!--<div class="col-12">-->
      <!--exportedGraphJson-->
      <!--<div>{{ exportedGraphJson }}</div>-->
      <!--</div>-->
      <!--</div>-->
      <!--<div class="row">-->
      <!--<div class="col-12">-->
      <!--apt of graph-->
      <!--<div style="text-align: left">-->
      <!--<pre><code>{{ graphApt }}</code></pre>-->
      <!--</div>-->
      <!--</div>-->
      <!--</div>-->
      <div class="row">
        <div class="col-12">
          <button class="btn btn-primary" v-on:click="refreshCytoscape">Refresh Cytoscape</button>
          <div>Cytoscape graph:</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
  //  import * as Vue from 'vue'
  import * as cytoscape from 'cytoscape'
  import * as cytoscapeCola from 'cytoscape-cola'
  import * as cytoscapeEuler from 'cytoscape-euler'
  import * as cytoscapeSpread from 'cytoscape-spread'
  import * as cytoscapeSpringy from 'cytoscape-springy'

  cytoscapeSpringy(cytoscape)
  cytoscapeSpread(cytoscape)

  cytoscapeCola(cytoscape) // Register the cola.js layout extension with cytoscape
  cytoscape.use(cytoscapeEuler) // Register the Euler layout extension

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
      },
      /**
       * Create a new instance of Cytoscape and embed it in the div #cy.
       * Set up all of our UI input event handlers.
       *
       * @param elements See http://js.cytoscape.org/#notation/elements-json
       */
      makeCytoscape: function (elements) {
        let cy

        const initialLayout = {
          name: 'null',
          fit: true,
          animate: false
        }
        cy = cytoscape({
          container: document.getElementById('cy'),
          elements: elements,
          minZoom: 0.5,
          maxZoom: 2,
          wheelSensitivity: 0.3,
          style: [ // the stylesheet for the graph
            {
              selector: 'node',
              style: {
                'label': 'data(id)'
//                'width': 10,
//                'height': 10
              }
            },
            {
              selector: '[?fixedByUser]',
              style: {
                'background-color': '#111'
              }
            },
            {
              selector: '[!fixedByUser]',
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
                'mid-target-arrow-color': '#000',
                'mid-target-arrow-shape': 'triangle'
              }
            }
          ],
          layout: initialLayout
        })
        console.log('initialized cytoscape: ' + cy)

//        function layoutNodes () {
//          const nodes = cy.nodes().filter(function (ele, i, eles) {
//            return ele.data('fixedByUser')
//          })
//          console.log('layoutUnfixedNodes: Locking fixed nodes: ')
//          console.log(nodes)
//          nodes.forEach(node => {
//            node.lock()
//          })
//          const layout = cy.layout({name: 'cose', fit: false})
//          const layout = cy.layout({name: 'cose', fit: false, animate: true, maxSimulationTime: 200})
//          let layout = cy.layout({
//            name: 'cose',
//            fit: false,
//            animate: true,
//            maxSimulationTime: 200
//          })
//          layout.run()
//          layout.on('layoutstop', function (event) {
//            console.log('layoutUnfixedNodes: Unlocking fixed nodes: ')
//            nodes.forEach(node => {
//              node.unlock()
//            })
//          })
//        }

        const layout = cy.layout({
          name: 'cola',
          animate: true,
          infinite: true,
          fit: false
        })
        layout.run()

        function layoutNodes () {

        }

//        const layout = cy.layout({ name: 'euler' })
//        layout.run()
//         Run the euler layout in an infinite loop
//        layout.on('layoutstop', function (event) {
//          console.log('restarting layout')
//          layout.run()
//        })
//        layout.pon('layoutstop').then(function (event) {
//          console.log('layout stop promise fulfilled')
//        })

        let self = this
        cy.nodes().on('click', function (event) {
          console.log(`Clicked node:${event.target.id()} with position x: ${event.target.position('x')}, y: ${event.target.position('y')}`)
//          console.log(`Unlocking node: ${event.target.id()}`)
//          event.target.unlock()
          // console.log(`Set fixedByUser=false for node id: ${event.target.id()}`)
          // event.target.data('fixedByUser', false)
          // layoutUnfixedNodes()
          // self.exportJson()
        })

        cy.nodes().on('drag', function (event) {
          console.log(`Event: Dragging node ${event.target.id()}`)
        })

        cy.nodes().on('cxttap', function (rightClickEvent) {
          console.log(`Event: Right clicked node ${rightClickEvent.target.id()}`)
          rightClickEvent.target.data('fixedByUser', false)
          layoutNodes()
          self.exportJson()
        })

        cy.nodes().on('free', function (event) {
          console.log(`Let go of node:${event.target.id()} with position x: ${event.target.position('x')}, y: ${event.target.position('y')}`)
          console.log(event)
          event.target.data('fixedByUser', true)
          console.log(`Set fixedByUser=true for node id: ${event.target.id()}`)
          layoutNodes()
          self.exportJson()
        })
        console.log('Added free event handler')

        return cy
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
  #graph-editor {
    height: 95vh;
    position: relative;
    text-align: center;
  }

  #overlay {
    z-index: 2;
    position: relative;
  }

  #cy {
    text-align: left;
    width: 100%;
    height: 100%;
    position: absolute;
    top: 0;
    left: 0;
    z-index: 1;
    /*display: flex;*/
    /*position: absolute;*/
    /*top: 0px;*/
    /*left: 0px;*/
  }
</style>
