// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import App from './App'

Vue.config.productionTip = false

/* eslint-disable no-new */
new Vue({
  el: '#app',
  // Use a hard-coded URL (http://localhost:4567) for all server requests when developing.
  // Otherwise, the requests would go to 'npm run dev's http server instead of our server.
  template: '<App v-bind:baseUrl=this.baseUrl />',
  data: function () {
    return {
      baseUrl: 'http://localhost:4567'
    }
  },
  components: { App }
})
