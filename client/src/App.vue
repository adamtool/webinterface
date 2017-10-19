<template>
  <div id='app' class="container-fluid">
    <div class="row">
      <div class="col-12">
        <button type="button" class="btn btn-primary pull-right" v-on:click="existsWinningStrategy">
          Exists winning strategy?
        </button>
        <button type="button" class="btn btn-primary pull-right" v-on:click="getStrategyBDD">Get Strategy BDD
        </button>
      </div>
    </div>
    <div class='row'>
      <div class='col-md-4'>
        <AptEditor v-bind:apt='apt' v-on:graphSaved='onAptSaved'></AptEditor>
      </div>
      <div class='col-md-8'>
        <tabs>
          <tab name="Petri Game">
            <GraphEditor v-bind:petriNet='petriGame.net' v-on:graphModified='onGraphModified'></GraphEditor>
          </tab>
          <tab name="Strategy BDD" v-if="petriGameHasStrategyBDD">
            <GraphEditor v-bind:petriNet='strategyBDD'></GraphEditor>
          </tab>
        </tabs>
      </div>
    </div>
  </div>
</template>

<script>
  import AptEditor from '@/components/AptEditor'
  import GraphEditor from '@/components/GraphEditor'
  import Vue from 'vue'
  import BootstrapVue from 'bootstrap-vue'
  import * as axios from 'axios'
  import {Tabs, Tab} from 'vue-tabs-component'
  Vue.component('tabs', Tabs)
  Vue.component('tab', Tab)

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
        apt: aptExample,
        strategyBDD: null
      }
    },
    computed: {
      petriGameHasStrategyBDD: function () {
        console.log('Do we have a strategy bdd?')
        const answer = this.strategyBDD !== null
        console.log('the answer is: ' + answer)
        return answer
      }
    },
    methods: {
      existsWinningStrategy: function () {
        axios.post('http://localhost:4567/existsWinningStrategy', {
          petriGameId: this.petriGame.uuid
        }).then(response => {
          console.log('Got response from existsWinningStrategy:')
          console.log(response.data)
        })
      },
      getStrategyBDD: function () {
        axios.post('http://localhost:4567/getStrategyBDD', {
          petriGameId: this.petriGame.uuid
        }).then(response => {
          console.log('Got response from getStrategyBDD:')
          console.log(response.data)
          this.strategyBDD = response.data.strategyBDD
        })
      },
      onGraphModified: function (graph) {
        console.log('App: Received graphModified event from graph editor:')
        console.log(graph)
        // TODO: Implement undo/redo.
      },
      onAptSaved: function (petriGame) {
        console.log('App: Got Petri Game from APT editor:')
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
