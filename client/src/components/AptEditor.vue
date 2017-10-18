<template>
  <div id="apt-editor">
    <div class="row">
      <div class="col-md-12">
        <h2>APT Editor</h2>
        <div>
          <textarea id="text-entry" class="form-control" v-model="textInput"></textarea>
        </div>
        <div>
          <button type="button" class="btn btn-primary pull-right" v-on:click="saveGraph">Send Graph to Editor</button>
        </div>
        Here is the graph we have received from the backend:
        <div id="petri-game-preview">
          {{ petriGamePreview }}
        </div>
      </div>
    </div>
  </div>
</template>

<script>
  //  import * as Vue from 'vue'

  import * as axios from 'axios'

  export default {
    name: 'apt-editor',
    components: {},
    props: ['apt'],
    data () {
      return {
        petriGamePreview: {
          net: {
            nodes: [],
            links: []
          },
          uuid: '123fakeuuid'
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
        axios.post('http://localhost:4567/convertAptToGraph', {
          params: {
            apt: this.textInput
          }
        }).then(response => {
          console.log('Received graph from backend:')
          console.log(response.data)
          this.petriGamePreview = response.data.graph
        })
      },
      saveGraph: function () {
        this.$emit('graphSaved', this.petriGamePreview)
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
  #apt-editor {
    text-align: center;
  }
</style>
