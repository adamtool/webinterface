<template>
  <div id='app' class="container-fluid">
    <div class="row">
      <div class="col-12">

      </div>
    </div>
    <div class='row'>
      <div class='col-md-4'>
        <AptEditor v-bind:apt='apt' v-on:graphSaved='onAptSaved'></AptEditor>
      </div>
      <div class='col-md-8'>
        <GraphEditor v-bind:petriNet='petriGame.net' v-on:graphModified='onGraphModified'></GraphEditor>
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
  import aptExample from './mutex.apt'

  export default {
    name: 'app',
    components: {
      'AptEditor': AptEditor,
      'GraphEditor': GraphEditor
    },
    data: function () {
      return {
        numberOfNodes: 50,
        numberOfEdges: 50,
        petriGame: {
          net: {
            links: [],
            nodes: []
          },
          uuid: 'abcfakeuuid123'
        },
        apt: aptExample
      }
    },
    methods: {
      onGraphModified: function (graph) {
        console.log('App: Received graphModified event from graph editor:')
        console.log(graph)
        // TODO: Implement undo/redo.
      },
      onAptSaved: function (petriGame) {
        console.log('App: Got graph from APT editor:')
        console.log(petriGame)
        this.petriGame = petriGame
      }
    }
  }
</script>

<style>
  #app {
    font-family: 'Avenir', Helvetica, Arial, sans-serif;
    -webkit-font-smoothing: antialiased;
    -moz-osx-font-smoothing: grayscale;
    color: #2c3e50;
    margin-top: 20px;
  }
</style>
