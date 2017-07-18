<template>
  <div id="app">
    <img src="./assets/logo.png">
    <div>Hi.  This text and the image above are in our main template, outside of the router.
      They will appear on every page.</div>


    <router-view></router-view>
  </div>
</template>

<script>
export default {
  name: 'app'
}
</script>

<style>
#app {
  font-family: 'Avenir', Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
  color: #2c3e50;
  margin-top: 60px;
}
</style>
