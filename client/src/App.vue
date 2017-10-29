<template>
  <div id='app' class="container-fluid">
    <div class='row'>
      <div class='col-md-4'>
        <AptEditor v-bind:apt='apt' v-on:graphSaved='onAptSaved'></AptEditor>
      </div>
      <div class='col-md-8'>
        <div class="row action-buttons">
          <div class="col-12">
            <button type="button" class="btn btn-primary" v-on:click="existsWinningStrategy">
              Exists winning strategy?
            </button>
            <button type="button" class="btn btn-primary" v-on:click="getStrategyBDD">Get Strategy BDD
            </button>
            <button type="button" class="btn btn-primary" v-on:click="getGraphStrategyBDD">Get Graph Strategy BDD
            </button>
          </div>
        </div>
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
  import * as iziToast from 'izitoast'
  import 'izitoast/dist/css/iziToast.min.css'
  import { Tabs, Tab } from 'vue-tabs-component'
  import './tabs-component.css'
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
    watch: {
      petriGame: function () {
        // The strategy BDD we have may no longer be valid after the Petri Game has changed, so we throw it out.
        // TODO Consider refactoring to use an immutable data structure.  "petriGame" and "strategyBDD" should maybe
        // be collapsed into one struct that is stored on the server.  This would prevent us from e.g.
        // accidentally having at the same time a strategy BDD and a Petri Game that do not match up.
        // (This can happen due to a race condition right now.  Try clicking "get strategy BDD" and then immediately
        // click "send graph to editor" before "getStrategyBDD" is finished running. You end up with a mismatched
        // combination of Petri Game and strategy BDD.
        this.strategyBDD = null
        this.switchToPetriGameTab()
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
      switchToStrategyBDDTab: function () {
        window.location.href = '#strategy-bdd'
      },
      switchToPetriGameTab: function () {
        window.location.href = '#petri-game'
      },
      getGraphStrategyBDD: function () {
        axios.post('http://localhost:4567/getGraphStrategyBDD', {
          petriGameId: this.petriGame.uuid
        }).then(response => {
          console.log('Got response from getGraphStrategyBDD:')
          console.log(response.data)
        })
      },
      existsWinningStrategy: function () {
        axios.post('http://localhost:4567/existsWinningStrategy', {
          petriGameId: this.petriGame.uuid
        }).then(response => {
          console.log('Got response from existsWinningStrategy:')
          console.log(response.data)
          // TODO error handling
          // TODO consider displaying the info in a more persistent way, e.g. by colorizing the button "exists winning strategy".
          // This is another piece of state that maybe should be kept on the server.
          if (response.data.result) {
            iziToast.show({
              color: 'green',
              timeout: 5000,
              message: 'Yes, there is a winning strategy for this Petri Game.',
              position: 'topRight'
            })
          } else {
            iziToast.show({
              color: 'red',
              timeout: 5000,
              message: 'No, there is no winning strategy for this Petri Game.',
              position: 'topRight'
            })
          }
        })
      },
      getStrategyBDD: function () {
        axios.post('http://localhost:4567/getStrategyBDD', {
          petriGameId: this.petriGame.uuid
        }).then(response => {
          // TODO Fix race condition here.  See above.
          // A quick fix would be to double-check the UUID of the petri game we have and reject the BDD
          // if the petri game's UUID has changed since the request was sent to the server, but I prefer the solution
          // described above.
          console.log('Got response from getStrategyBDD:')
          console.log(response.data)
          this.strategyBDD = response.data.strategyBDD
          this.switchToStrategyBDDTab()
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

  .action-buttons {
    /*text-align: center;*/
  }
</style>
