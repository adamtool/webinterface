<template>
  <div class="apt-editor">
    <textarea class="form-control" v-model="textEditorContents"></textarea>
    <div class="buttons">
      <div>
        <input type="file" class="file-input" :id="filePickerUID" v-on:change="onFileSelected">
        <label class="btn btn-primary file-input-label" :for="filePickerUID">Load APT from
          file</label>
      </div>
      <button type="button" class="btn btn-primary" v-on:click="saveAptToFile">
        Save APT to file
      </button>
    </div>
  </div>
</template>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
  .buttons {
    display: flex;
    flex-direction: row;
    justify-content: space-between;
  }

  .apt-editor {
    height: 85vh;
    text-align: center;
    display: flex;
    flex-direction: column;
    flex: 0.55;
  }

  .apt-editor textarea {
    box-sizing: border-box;
    flex: 1;
    height: 100%;
    width: 100%;
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
    props: ['apt'],
    data () {
      return {
        textEditorContents: this.apt
      }
    },
    computed: {
      filePickerUID: function () {
        return 'file-picker-' + this._uid
      }
    },
    methods: {
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
