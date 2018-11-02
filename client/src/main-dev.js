import { createVue } from './main.base'

createVue({
  baseUrl: 'http://localhost:4567',
  // These are compiler macros that are defined using webpack.DefinePlugin
  // in webpack.base.conf.js
// eslint-disable-next-line no-undef
  useModelChecking: ADAMWEB_USE_MODEL_CHECKING,
// eslint-disable-next-line no-undef
  useOtherApproach: ADAMWEB_USE_OTHER_APPROACH
})
