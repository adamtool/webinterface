// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import MainRouterView from './MainRouterView'
import router from './router'

Vue.config.productionTip = false

export {
  createVue
}

/**
 * @param baseUrl The URL prefix for all of our requests to the server
 * @param mode Either 'MODEL_CHECKING', 'OTHER_APPROACH', or 'MODEL_CHECKING_AND_OTHER_APPROACH'.
 * TODO Rename "OTHER_APPROACH"
 */
function createVue ({baseUrl, mode}) {
  const validModes = ['MODEL_CHECKING', 'OTHER_APPROACH', 'MODEL_CHECKING_AND_OTHER_APPROACH']
  if (!(validModes.includes(mode))) {
    throw new Error(`Unrecognized value for ADAMWEB_MODE: ${mode}\nValid modes: [${validModes.join(', ')}]`)
  }
  const useModelChecking = mode === 'MODEL_CHECKING_AND_OTHER_APPROACH' || mode === 'MODEL_CHECKING'
  const useOtherApproach = mode === 'MODEL_CHECKING_AND_OTHER_APPROACH' || mode === 'OTHER_APPROACH'
  /* eslint-disable no-new */
  new Vue({
    el: '#app',
    router,
    // Use relative URLs for server requests
    template: '<MainRouterView/>',
    data: function () {
      return {
        baseUrl,
        useModelChecking,
        useOtherApproach
      }
    },
    components: {MainRouterView}
  })
}
