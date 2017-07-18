<template>
  <div id="apt-editor">
    <h2>Editor</h2>
    Here is the text in the editor: <br>
    {{ content }}
    <quill-editor v-model="content"
                  ref="myQuillEditor"
                  :options="editorOption"
                  @blur="onEditorBlur($event)"
                  @focus="onEditorFocus($event)"
                  @ready="onEditorReady($event)">
    </quill-editor>

  </div>
</template>

<script>
  import { quillEditor } from 'vue-quill-editor'
  export default {
    name: 'apt-editor',
    components: {
      quillEditor
    },
    data () {
      return {
        content: 'default text to appear inside of editor',
        editorOption: {
          theme: 'snow',
          modules: {
            toolbar: ['bold', 'italic', 'underline']
          }
        }
      }
    }
  }
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
</style>
