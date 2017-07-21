<template>
  <div id="graph-editor">
    <h2>Graph Editor</h2>
    parentGraph (A property given to us by our parent):
    <div>{{ parentGraph }}</div>
    graphDrawn (A data element that belongs to us):
    <div>{{ graphDrawn }}</div>
  </div>
</template>

<script>
  //  import * as Vue from 'vue'

//  import * as axios from 'axios'
//  import * as MockAdapter from 'axios-mock-adapter'

//  let mock = new MockAdapter(axios)

  export default {
    name: 'graph-editor',
    components: {},
    data () {
      return {
        graphDrawn: this.parentGraph
      }
    },
    methods: {
      renderGraph: function () {
      },
      saveGraph: function () {
      }
    },
    props: ['parentGraph'],
    watch: {
      parentGraph: function (graph) {
        /* When parentGraph changes, this should mean the user did one of the following things:
        1. Changed something in the APT editor, causing the APT to be parsed on the server, yielding a new graph

        In response, we will update the graph that is being edited in the drag-and-drop GUI of this component.
         */
        this.graphDrawn = graph
      }
    }
  }
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
</style>
