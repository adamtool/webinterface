<template>
  <App useModelChecking
       :useDistributedSynthesis="false"/>
</template>

<script>
  import App from './App'

  export default {
    name: 'app-model-checking',
    components: {
      App
    }
  }
</script>

<style scoped>

</style>
