// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import App from './App'

Vue.config.productionTip = false

export {
  createVue
}

function createVue (config) {
  /* eslint-disable no-new */
  new Vue({
    el: '#app',
    // Use relative URLs for server requests
    template: '<App ' +
    'v-bind:baseUrl=this.config.baseUrl ' +
    'v-bind:useModelChecking=this.config.useModelChecking ' +
    'v-bind:useOtherApproach=this.config.useOtherApproach />',
    data: function () {
      return {
        config: config
      }
    },
    components: {App}
  })
}
