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

  svg {
    background-color: #FFF;
    cursor: default;
    -webkit-user-select: none;
    -moz-user-select: none;
    -ms-user-select: none;
    -o-user-select: none;
    user-select: none;
  }

  svg:not(.active):not(.ctrl) {
    cursor: crosshair;
  }

  path.link {
    fill: none;
    stroke: #000;
    stroke-width: 4px;
    cursor: default;
  }

  svg:not(.active):not(.ctrl) path.link {
    cursor: pointer;
  }

  path.link.selected {
    stroke-dasharray: 10,2;
  }

  path.link.dragline {
    pointer-events: none;
  }

  path.link.hidden {
    stroke-width: 0;
  }

  circle.node {
    stroke-width: 1.5px;
    cursor: pointer;
  }

  circle.node.reflexive {
    stroke: #000 !important;
    stroke-width: 2.5px;
  }

  text {
    font: 12px sans-serif;
    pointer-events: none;
  }

  text.id {
    text-anchor: middle;
    font-weight: bold;
  }
</style>

<script>
  import * as d3 from 'd3'

  export default {
    name: 'graph-editor',
    components: {},
    data () {
      return {
        exportedGraphJson: {}
      }
    },
    mounted: function () {
      this.initializeD3()
    },
    methods: {
      initializeD3: function () {
        // set up SVG for D3
        var width = 960
        var height = 500
        var colors = d3.scale.category10()

        var svg = d3.select('#graph')
          .append('svg')
          .attr('oncontextmenu', 'return false;')
          .attr('width', '100%')
          .attr('height', '100%')

        // set up initial nodes and links
        //  - nodes are known by 'id', not by index in array.
        //  - reflexive edges are indicated on the node (as a bold black circle).
        //  - links are always source < target; edge directions are set by 'left' and 'right'.
        var nodes = [
          {id: 0, reflexive: false},
          {id: 1, reflexive: true},
          {id: 2, reflexive: false}
        ]
        var lastNodeId = 2
        var links = [
          {source: nodes[0], target: nodes[1], left: false, right: true},
          {source: nodes[1], target: nodes[2], left: false, right: true}
        ]

        // init D3 force layout
        var force = d3.layout.force()
          .nodes(nodes)
          .links(links)
          .size([width, height])
          .linkDistance(150)
          .charge(-500)
          .on('tick', tick)

        // define arrow markers for graph links
        svg.append('svg:defs').append('svg:marker')
          .attr('id', 'end-arrow')
          .attr('viewBox', '0 -5 10 10')
          .attr('refX', 6)
          .attr('markerWidth', 3)
          .attr('markerHeight', 3)
          .attr('orient', 'auto')
          .append('svg:path')
          .attr('d', 'M0,-5L10,0L0,5')
          .attr('fill', '#000')

        svg.append('svg:defs').append('svg:marker')
          .attr('id', 'start-arrow')
          .attr('viewBox', '0 -5 10 10')
          .attr('refX', 4)
          .attr('markerWidth', 3)
          .attr('markerHeight', 3)
          .attr('orient', 'auto')
          .append('svg:path')
          .attr('d', 'M10,-5L0,0L10,5')
          .attr('fill', '#000')

        // line displayed when dragging new nodes
        var dragLine = svg.append('svg:path')
          .attr('class', 'link dragline hidden')
          .attr('d', 'M0,0L0,0')

        // handles to link and node element groups
        var path = svg.append('svg:g').selectAll('path')
        var circle = svg.append('svg:g').selectAll('g')

        // mouse event vars
        var selectedNode = null
        var selectedLink = null
        var mousedownLink = null
        var mousedownNode = null
        var mouseupNode = null

        function resetMouseVars () {
          mousedownNode = null
          mouseupNode = null
          mousedownLink = null
        }

        // update force layout (called automatically each iteration)
        function tick () {
          // draw directed edges with proper padding from node centers
          path.attr('d', function (d) {
            var deltaX = d.target.x - d.source.x
            var deltaY = d.target.y - d.source.y
            var dist = Math.sqrt(deltaX * deltaX + deltaY * deltaY)
            var normX = deltaX / dist
            var normY = deltaY / dist
            var sourcePadding = d.left ? 17 : 12
            var targetPadding = d.right ? 17 : 12
            var sourceX = d.source.x + (sourcePadding * normX)
            var sourceY = d.source.y + (sourcePadding * normY)
            var targetX = d.target.x - (targetPadding * normX)
            var targetY = d.target.y - (targetPadding * normY)
            return 'M' + sourceX + ',' + sourceY + 'L' + targetX + ',' + targetY
          })

          circle.attr('transform', function (d) {
            return 'translate(' + d.x + ',' + d.y + ')'
          })
        }

        // update graph (called when needed)
        function restart () {
          // path (link) group
          path = path.data(links)

          // update existing links
          path.classed('selected', function (d) { return d === selectedLink })
            .style('marker-start', function (d) { return d.left ? 'url(#start-arrow)' : '' })
            .style('marker-end', function (d) { return d.right ? 'url(#end-arrow)' : '' })

          // add new links
          path.enter().append('svg:path')
            .attr('class', 'link')
            .classed('selected', function (d) { return d === selectedLink })
            .style('marker-start', function (d) { return d.left ? 'url(#start-arrow)' : '' })
            .style('marker-end', function (d) { return d.right ? 'url(#end-arrow)' : '' })
            .on('mousedown', function (d) {
              if (d3.event.ctrlKey) return

              // select link
              mousedownLink = d
              if (mousedownLink === selectedLink) selectedLink = null
              else selectedLink = mousedownLink
              selectedNode = null
              restart()
            })

          // remove old links
          path.exit().remove()

          // circle (node) group
          // NB: the function arg is crucial here! nodes are known by id, not by index!
          circle = circle.data(nodes, function (d) { return d.id })

          // update existing nodes (reflexive & selected visual states)
          circle.selectAll('circle')
            .style('fill', function (d) { return (d === selectedNode) ? d3.rgb(colors(d.id)).brighter().toString() : colors(d.id) })
            .classed('reflexive', function (d) { return d.reflexive })

          // add new nodes
          var g = circle.enter().append('svg:g')

          g.append('svg:circle')
            .attr('class', 'node')
            .attr('r', 12)
            .style('fill', function (d) { return (d === selectedNode) ? d3.rgb(colors(d.id)).brighter().toString() : colors(d.id) })
            .style('stroke', function (d) { return d3.rgb(colors(d.id)).darker().toString() })
            .classed('reflexive', function (d) { return d.reflexive })
            .on('mouseover', function (d) {
              if (!mousedownNode || d === mousedownNode) return
              // enlarge target node
              d3.select(this).attr('transform', 'scale(1.1)')
            })
            .on('mouseout', function (d) {
              if (!mousedownNode || d === mousedownNode) return
              // unenlarge target node
              d3.select(this).attr('transform', '')
            })
            .on('mousedown', function (d) {
              if (d3.event.ctrlKey) return

              // select node
              mousedownNode = d
              if (mousedownNode === selectedNode) selectedNode = null
              else selectedNode = mousedownNode
              selectedLink = null

              // reposition drag line
              dragLine
                .style('marker-end', 'url(#end-arrow)')
                .classed('hidden', false)
                .attr('d', 'M' + mousedownNode.x + ',' + mousedownNode.y + 'L' + mousedownNode.x + ',' + mousedownNode.y)

              restart()
            })
            .on('mouseup', function (d) {
              if (!mousedownNode) return

              // needed by FF
              dragLine
                .classed('hidden', true)
                .style('marker-end', '')

              // check for drag-to-self
              mouseupNode = d
              if (mouseupNode === mousedownNode) {
                resetMouseVars()
                return
              }

              // unenlarge target node
              d3.select(this).attr('transform', '')

              // add link to graph (update if exists)
              // NB: links are strictly source < target; arrows separately specified by booleans
              var source, target, direction
              if (mousedownNode.id < mouseupNode.id) {
                source = mousedownNode
                target = mouseupNode
                direction = 'right'
              } else {
                source = mouseupNode
                target = mousedownNode
                direction = 'left'
              }

              var link
              link = links.filter(function (l) {
                return (l.source === source && l.target === target)
              })[0]

              if (link) {
                link[direction] = true
              } else {
                link = {source: source, target: target, left: false, right: false}
                link[direction] = true
                links.push(link)
              }

              // select new link
              selectedLink = link
              selectedNode = null
              restart()
            })

          // show node IDs
          g.append('svg:text')
            .attr('x', 0)
            .attr('y', 4)
            .attr('class', 'id')
            .text(function (d) { return d.id })

          // remove old nodes
          circle.exit().remove()

          // set the graph in motion
          force.start()
        }

        function mousedown () {
          // prevent I-bar on drag
          // d3.event.preventDefault();

          // because :active only works in WebKit?
          svg.classed('active', true)

          if (d3.event.ctrlKey || mousedownNode || mousedownLink) return

          // insert new node at point
          var point = d3.mouse(this)
          var node = {id: ++lastNodeId, reflexive: false}
          node.x = point[0]
          node.y = point[1]
          nodes.push(node)

          restart()
        }

        function mousemove () {
          if (!mousedownNode) return

          // update drag line
          dragLine.attr('d', 'M' + mousedownNode.x + ',' + mousedownNode.y + 'L' + d3.mouse(this)[0] + ',' + d3.mouse(this)[1])

          restart()
        }

        function mouseup () {
          if (mousedownNode) {
            // hide drag line
            dragLine
              .classed('hidden', true)
              .style('marker-end', '')
          }

          // because :active only works in WebKit?
          svg.classed('active', false)

          // clear mouse event vars
          resetMouseVars()
        }

        function spliceLinksForNode (node) {
          var toSplice = links.filter(function (l) {
            return (l.source === node || l.target === node)
          })
          toSplice.map(function (l) {
            links.splice(links.indexOf(l), 1)
          })
        }

        // only respond once per keydown
        var lastKeyDown = -1

        function keydown () {
          d3.event.preventDefault()

          if (lastKeyDown !== -1) return
          lastKeyDown = d3.event.keyCode

          // ctrl
          if (d3.event.keyCode === 17) {
            circle.call(force.drag)
            svg.classed('ctrl', true)
          }

          if (!selectedNode && !selectedLink) return
          switch (d3.event.keyCode) {
            case 8: // backspace
            case 46: // delete
              if (selectedNode) {
                nodes.splice(nodes.indexOf(selectedNode), 1)
                spliceLinksForNode(selectedNode)
              } else if (selectedLink) {
                links.splice(links.indexOf(selectedLink), 1)
              }
              selectedLink = null
              selectedNode = null
              restart()
              break
            case 66: // B
              if (selectedLink) {
                // set link direction to both left and right
                selectedLink.left = true
                selectedLink.right = true
              }
              restart()
              break
            case 76: // L
              if (selectedLink) {
                // set link direction to left only
                selectedLink.left = true
                selectedLink.right = false
              }
              restart()
              break
            case 82: // R
              if (selectedNode) {
                // toggle node reflexivity
                selectedNode.reflexive = !selectedNode.reflexive
              } else if (selectedLink) {
                // set link direction to right only
                selectedLink.left = false
                selectedLink.right = true
              }
              restart()
              break
          }
        }

        function keyup () {
          lastKeyDown = -1

          // ctrl
          if (d3.event.keyCode === 17) {
            circle
              .on('mousedown.drag', null)
              .on('touchstart.drag', null)
            svg.classed('ctrl', false)
          }
        }

        // app starts here
        svg.on('mousedown', mousedown)
          .on('mousemove', mousemove)
          .on('mouseup', mouseup)
        d3.select(window)
          .on('keydown', keydown)
          .on('keyup', keyup)
        restart()
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
        this.initializeD3()
      }
    }
  }

</script>
