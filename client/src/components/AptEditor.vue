<template>
  <div class="apt-editor">
    <div class="text-editor">
      <textarea class="form-control" v-model="textEditorContents"></textarea>
      <div class="buttons">
        <div>
          <input type="file" class="file-input" :id="filePickerUID" v-on:change="onFileSelected">
          <label class="btn btn-primary file-input-label" :for="filePickerUID">Load APT from file</label>
        </div>
        <button type="button" class="btn btn-primary" v-on:click="saveAptToFile">
          Save APT to file
        </button>
      </div>
    </div>

  </div>
</template>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
  .apt-editor {
  }

  .buttons {
    display: flex;
    flex-direction: row;
    justify-content: space-between;
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

  /*Hide the ugly file input element*/
  .file-input {
    width: 0.1px;
    height: 0.1px;
    opacity: 0;
    overflow: hidden;
    position: absolute;
    z-index: -1;
  }

  .file-input-label {
    display: table-cell;
  }
</style>

<script>
  //  import * as Vue from 'vue'

  export default {
    name: 'apt-editor',
    components: {},
    // Passing in the URL as a property is kludgy.
    // TODO Refactor so that HTTP requests take place in App.vue rather than here
    props: ['apt', 'convertAptToGraphUrl'],
    data () {
      return {
        petriGameFromServer: {
          net: {
            nodes: [],
            links: []
          },
          uuid: '123fakeuuid'
        },
        textEditorContents: this.apt,
        serverResponse: {}
      }
    },
    computed: {
      serverResponsePrettyPrinted: function () {
        return JSON.stringify(this.serverResponse, null, 2)
      },
      filePickerUID: function () {
        return 'file-picker-' + this._uid
      }
    },
    mounted: function () {
    },
    methods: {
      // Load APT from a text file stored on the user's local filesystem
      // See https://developer.mozilla.org/en-US/docs/Web/API/File/Using_files_from_web_applications
      onFileSelected: function (changeEvent) {
        console.log('The user selected a file in the file selector')
        const file = changeEvent.target.files[0]
        console.log(file)
        const reader = new FileReader()
        reader.onloadend = () => {
          // TODO verify that the file is reasonable (i.e. plain text, not a binary or other weird file)
          console.log('The file selected by the user is finished loading.  Updating text editor contents')
          this.textEditorContents = reader.result
        }
        reader.readAsText(file)
      },
      saveAptToFile: function () {
        // This function is adapted from https://stackoverflow.com/a/21016088
        const data = new Blob([this.textEditorContents], {type: 'text/plain'})
        const textFileUrl = window.URL.createObjectURL(data)
        const link = document.createElement('a')
        link.setAttribute('download', 'apt.txt')
        link.href = textFileUrl
        document.body.appendChild(link)
        // wait for the link to be added to the document
        window.requestAnimationFrame(function () {
          const event = new MouseEvent('click')
          link.dispatchEvent(event)
          document.body.removeChild(link)
          // Prevent memory leak by revoking the URL after it has been downloaded
          window.URL.revokeObjectURL(textFileUrl)
        })
      }
    },
    watch: {
      apt: function (newApt) {
        console.log('Updating text editor contents')
        this.textEditorContents = newApt
      },
      textEditorContents: function (apt) {
        this.$emit('textEdited', apt)
      }
    }
  }
</script>
