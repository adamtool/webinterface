<template>
  <div id="apt-editor">
    <h2>Editor</h2>
    Here is the text in the editor:
    <div id="editor-content">
      {{ content }}
    </div>
    Here is the graph we have:
    <div id="graph">
      {{ graph }}
    </div>
    <div>
      <textarea id="text-entry" class="form-control" v-model="content"></textarea>
      <button type="button" class="btn btn-primary pull-right" @click="renderGraph">Render Graph</button>
    </div>
  </div>
</template>

<script>
//  import * as Vue from 'vue'

  import * as axios from 'axios'
  import * as MockAdapter from 'axios-mock-adapter'
  let mock = new MockAdapter(axios)

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

  export default {
    name: 'apt-editor',
    components: {},
    data () {
      return {
        graph: {
          nodes: [],
          edges: []
        },
        content: '# Type APT into here.  \nClick "Send" to send it to ADAM and turn it into a graph.'
      }
    },
    methods: {
      renderGraph: function () {
        let vm = this
        axios.post('/aptToGraph', {
          params: {
            aptSourceCode: '1 -> 2'
          }
        }).then(response => {
          console.log(response.data)
//          TODO: Find out how to use the response in Vue
          vm.$set(this.data, 'graph', response.data.graph)
        })
      }
    }
  }
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
</style>
