// TODO Figure out how to read these environment variables during compilation
// and then pass them along to the JS code that runs clientside.
// Some ideas here: https://stackoverflow.com/questions/30030031/passing-environment-dependent-variables-in-webpack
module.exports = {
  useModelChecking: !!process.env.ADAMWEB_USE_MODEL_CHECKING,
  useOtherApproach: !!process.env.ADAMWEB_USE_OTHER_APPROACH,
  someOtherVar: 'Hi there :)'
}
