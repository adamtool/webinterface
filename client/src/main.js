// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import MainRouterView from './MainRouterView'
import router from './router'
import Vuetify from 'vuetify'
import '@mdi/font/css/materialdesignicons.css'

Vue.config.productionTip = false

Vue.use(Vuetify)

const opts = {}

/* eslint-disable no-new */
new Vue({
  el: '#app',
  router,
  // Use relative URLs for server requests
  template: '<MainRouterView/>',
  components: { MainRouterView },
  vuetify: new Vuetify(opts)
})
