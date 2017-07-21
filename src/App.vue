<template>
  <div id="app">
    <div class="row">
      <div class="col-md-12">
        Hi.  This is the header of our main template in App.vue.
      </div>
    </div>
    <div class="row">
      <div class="col-md-6">
        <AptEditor v-on:graphSaved='onAptSaved'></AptEditor>
      </div>
      <div class="col-md-6">
        <GraphEditor v-bind:parentGraph='graph'></GraphEditor>
      </div>
    </div>
  </div>
</template>

<script>
  import AptEditor from '@/components/AptEditor'
  import GraphEditor from '@/components/GraphEditor'
  import Vue from 'vue'
  import BootstrapVue from 'bootstrap-vue'

  Vue.use(BootstrapVue)
  import 'bootstrap/dist/css/bootstrap.css'
  import 'bootstrap-vue/dist/bootstrap-vue.css'

  export default {
    name: 'app',
    components: {
      'AptEditor': AptEditor,
      'GraphEditor': GraphEditor
    },
    data: function () {
      return {
        graph: {
          nodes: [1, 2, 3],
          edges: [[1, 2], [2, 3], [3, 1]]
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
