<template>
  <App :useModelChecking="false"
       useDistributedSynthesis/>
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
