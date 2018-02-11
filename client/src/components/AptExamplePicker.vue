Present a dropdown menu with a list of APT examples.
When an example is selected by the user, emit the event 'fileSelected' with the APT text as its payload.
<template>
   <div class="apt-example-picker">
     <select v-model="selected">
       <option disabled value="">Select an APT example to load</option>
       <option v-for="(apt, filename) in aptFiles" v-bind:value="apt">
         {{ filename }}
       </option>
     </select>
     <!--<div>-->
       <!--Selected: <pre>{{ selected }}</pre>-->
     <!--</div>-->
   </div>
</template>

<script>
  // Import all the APT files.  This is a little bit complicated if you've never seen it before.
  // See https://webpack.js.org/guides/dependency-management/#require-with-expression
  const aptFilesContext = require.context('../assets/apt-examples', true, /\.apt$/)
  const aptFiles = {}
  function importAll (r) {
    r.keys().forEach(key => {
      aptFiles[key] = r(key)
    })
  }
  importAll(aptFilesContext)

  export default {
    name: 'apt-example-picker',
    data () {
      return {
        aptFiles: aptFiles,
        selected: ''
      }
    },
    watch: {
      selected: function (apt) {
        this.$emit('fileSelected', apt)
      }
    }
  }
</script>

<style scoped>

</style>
