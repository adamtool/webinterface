<template>
  <div id="apt-editor">
    <div class="row">
      <div class="col-md-12">
        <h2>APT Editor</h2>
        Here is the graph we have received from the backend: (Note that at the moment, the backend is being mocked, and
        will only respond to the exact string "1 -> 2" -- Otherwise, it will simply return an empty graph.)
        <div id="graph-preview">
          {{ graphPreview }}
        </div>
        <div>
          <textarea id="text-entry" class="form-control" v-model="textInput"></textarea>
          <button type="button" class="btn btn-primary pull-right" v-on:click="saveGraph">Send Graph to Editor</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
  //  import * as Vue from 'vue'

  import * as axios from 'axios'
  import * as MockAdapter from 'axios-mock-adapter'

  let mock = new MockAdapter(axios, {
//    delayResponse: 100
  })

  mock.onPost('/aptToGraph', {
    params: {
      aptSourceCode: '1 -> 2'
    }
  }).reply(200, {
    graph: {
      nodes: [1, 2],
      edges: [[1, 2]]
    }
  })

  mock.onPost('/aptToGraph').reply(200, {
    graph: {
      nodes: [],
      edges: []
    }
  })

  export default {
    name: 'apt-editor',
    components: {},
    props: ['apt'],
    data () {
      return {
        graphPreview: {
          nodes: [],
          edges: []
        },
        textInput: this.apt
      }
    },
    mounted: function () {
      this.renderGraph()
    },
    methods: {
      renderGraph: function () {
        console.log('Sending APT source code to backend: ' + this.textInput)
        axios.post('/aptToGraph', {
          params: {
            aptSourceCode: this.textInput
          }
        }).then(response => {
          console.log('Received graph from backend:')
          console.log(response.data)
          this.graphPreview = response.data.graph
        })
      },
      saveGraph: function () {
        this.$emit('graphSaved', this.graphPreview)
      }
    },
    watch: {
      apt: function (newApt) {
        console.log('Updating text editor contents')
        this.textInput = newApt
      },
      textInput: function (newText) {
        console.log('textInput("' + newText + '")')
        this.renderGraph()
      }
    }
  }
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
  #text-entry {
    height: 350px;
  }
</style>
