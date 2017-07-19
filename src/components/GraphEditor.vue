<template>
  <div id="graph-editor">
    <h2>Graph Editor</h2>
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
        graphPreview: {
          nodes: [],
          edges: []
        },
        graph: {
          nodes: [],
          edges: []
        }
      }
    },
    methods: {
      renderGraph: function () {
      },
      saveGraph: function () {
      }
    },
    watch: {
    }
  }
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
</style>
