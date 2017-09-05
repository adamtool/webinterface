<template>
  <div id='app' class="container-fluid">
    <div class="row">
      <div class="col-12">

      </div>
    </div>
    <div class='row'>
      <div class='col-md-4'>
        <div>Number of nodes
          <input type="number" v-model="numberOfNodes">
        </div>
        <div>Number of edges
          <input type="number" v-model="numberOfEdges">
        </div>
        <div>
          <button v-on:click="replaceGraph">Create new random graph</button>
        </div>
        <AptEditor v-bind:apt='apt' v-on:graphSaved='onAptSaved'></AptEditor>
      </div>
      <div class='col-md-8'>
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
        numberOfNodes: 50,
        numberOfEdges: 50,
        graph: this.makeRandomGraph(this.numberOfNodes, this.numberOfEdges),
        apt: '1 -> 2'
      }
    },
    methods: {
      replaceGraph: function () {
        this.graph = this.makeRandomGraph(this.numberOfNodes, this.numberOfEdges)
      },
      makeRandomGraph: function (numberOfNodes, numberOfEdges) {
        const nodes = []
        for (let i = 0; i < numberOfNodes; i++) {
          nodes.push(i)
        }
        const edges = []
        for (let i = 0; i < numberOfEdges; i++) {
          let randomNode = function () {
            return Math.floor((Math.random() * numberOfNodes - 1) + 1)
          }
          let first = randomNode()
          let second = randomNode()
          while (second === first) {
            second = randomNode()
          }
          edges.push([first, second])
        }
        return {
          nodes: nodes,
          edges: edges
        }
      },
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
    margin-top: 20px;
  }
</style>
