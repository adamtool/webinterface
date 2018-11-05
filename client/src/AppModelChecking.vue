<template>
  <!--TODO Make baseUrl different for dev / production-->
  <!--baseUrl="http://localhost:4567"-->
  <App baseUrl="http://localhost:4567"
       useModelChecking
       :useOtherApproach="false"/>
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
