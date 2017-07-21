<template>
  <div id="app">
    <img src="./assets/logo.png">
    <div>Hi.  This text and the image above are in our main template, outside of the router.
      They will appear on every page.
    </div>

    <AptEditor v-on:graphSaved='onAptSaved'></AptEditor>
    <GraphEditor v-bind:parentGraph='graph'></GraphEditor>
  </div>
</template>

<script>
  import AptEditor from '@/components/AptEditor'
  import GraphEditor from '@/components/GraphEditor'

  export default {
    name: 'app',
    components: {
      'AptEditor': AptEditor,
      'GraphEditor': GraphEditor
    },
    data: function () {
      return {
        graph: {
          nodes: [],
          edges: []
        }
      }
    },
    methods: {
      onAptSaved: function (graph) {
        console.log('Got graph from APT editor:')
        console.log(graph)
        this.graph = graph
      }
    }
  }
</script>

<style>
  #app {
    font-family: 'Avenir', Helvetica, Arial, sans-serif;
    -webkit-font-smoothing: antialiased;
    -moz-osx-font-smoothing: grayscale;
    text-align: center;
    color: #2c3e50;
    margin-top: 60px;
  }
</style>
