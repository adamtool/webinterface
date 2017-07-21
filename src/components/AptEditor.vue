<template>
  <div id="apt-editor">
    <div class="row">
      <div class="col-md-12">
        <h2>APT Editor</h2>
        Here is the text in the editor:
        <div id="editor-content">
          {{ textInput }}
        </div>
        Here is the graph we have:
        <div id="saved-graph">
          {{ graph }}
        </div>
        Here is the graph we are previewing:
        <div id="graph-preview">
          {{ graphPreview }}
        </div>
        <div>
          <textarea id="text-entry" class="form-control" v-model="textInput"></textarea>
          <button type="button" class="btn btn-primary pull-right" v-on:click="saveGraph">Save Graph</button>
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
    data () {
      return {
        graphPreview: {
          nodes: [],
          edges: []
        },
        graph: {
          nodes: [],
          edges: []
        },
        textInput: '1 -> 2'
      }
    },
    methods: {
      renderGraph: function () {
        axios.post('/aptToGraph', {
          params: {
            aptSourceCode: this.textInput
          }
        }).then(response => {
          console.log(response.data)
          this.graphPreview = response.data.graph
        })
      },
      saveGraph: function () {
        this.graph = this.graphPreview
        this.$emit('graphSaved', this.graph)
      }
    },
    watch: {
      textInput: function (oldText, newText) {
        console.log('textInput(' + oldText + ', ' + newText + ')')
        if (oldText !== newText) {
          this.renderGraph()
        }
      }
    }
  }
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
</style>
