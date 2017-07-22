<template>
  <div id='app'>
    <div class='row'>
      <div class='col-md-12'>
        Hi.  This is the header of our main template in App.vue.
      </div>
    </div>
    <div class='row'>
      <div class='col-md-6'>
        <AptEditor v-bind:apt='apt' v-on:graphSaved='onAptSaved'></AptEditor>
      </div>
      <div class='col-md-6'>
        <GraphEditor v-bind:parentGraph='graph' v-on:graphModified='onGraphModified'></GraphEditor>
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
        },
        apt: '1 -> 2'
      }
    },
    methods: {
      onGraphModified: function (apt) {
        console.log('Got APT from graph editor:')
        console.log(apt)
        this.apt = apt
      },
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
