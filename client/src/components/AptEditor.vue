<template>
  <div class="apt-editor">
    <div class="text-editor">
      <textarea class="form-control" v-model="textInput"></textarea>
      <div>
        <button type="button" class="btn btn-primary pull-right" v-on:click="saveGraph">Send Graph to Editor</button>
      </div>
    </div>
    <div class="debug-output">
      <h2>Debug output</h2>
      <div id="server-response">
        <template v-if="serverResponse.status === 'success'">
          Here is the result of parsing the APT:
          <pre>{{ serverResponsePrettyPrinted }}</pre>
        </template>
        <template v-else-if="serverResponse.status === 'error'">
          There was an error when we tried to parse the APT:
          <pre>{{ serverResponse.message }}</pre>
        </template>
        <template v-else>
          We got a totally malformed response from the server:
          <pre>{{ serverResponse }}</pre>
        </template>
      </div>
    </div>
  </div>
</template>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
  .apt-editor {
    display: flex;
    flex-direction: row;
  }

  .text-editor {
    height: 85vh;
    text-align: center;
    display: flex;
    flex-direction: column;
    flex: 0.55;
  }

  .text-editor textarea {
    box-sizing: border-box;
    flex: 1;
    height: 100%;
    width: 100%;
  }

  .debug-output {
    margin: 10px;
    flex: 0.45;
  }
</style>

<script>
  //  import * as Vue from 'vue'

  import * as axios from 'axios'

  export default {
    name: 'apt-editor',
    components: {},
    props: ['apt'],
    data () {
      return {
        petriGameFromServer: {
          net: {
            nodes: [],
            links: []
          },
          uuid: '123fakeuuid'
        },
        textInput: this.apt,
        serverResponse: {}
      }
    },
    computed: {
      serverResponsePrettyPrinted: function () {
        return JSON.stringify(this.serverResponse, null, 2)
      }
    },
    mounted: function () {
      this.renderGraph()
    },
    methods: {
      renderGraph: function () {
        console.log('Sending APT source code to backend.')
        axios.post('http://localhost:4567/convertAptToGraph', {
          params: {
            apt: this.textInput
          }
        }).then(response => {
          switch (response.data.status) {
            case 'success':
              console.log('Received graph from backend:')
              console.log(response.data)
              this.serverResponse = response.data
              this.petriGameFromServer = response.data.graph
              break
            default:
              this.serverResponse = response.data
              break
          }
          // TODO handle broken connection to server
        })
      },
      saveGraph: function () {
        this.$emit('graphSaved', this.petriGameFromServer)
      }
    },
    watch: {
      apt: function (newApt) {
        console.log('Updating text editor contents')
        this.textInput = newApt
      },
      textInput: function () {
        this.renderGraph()
      }
    }
  }
</script>
