// Usage:
// import * as mcroutes from 'modelCheckingRoutes'
// const modelCheckingRoutes = mcroutes.withPathPrefix('localhost:8080')
// [...]
// modelCheckingRoutes.checkLtlFormula(someId, someFormula)

import * as axios from 'axios'

export default {withPathPrefix, noOpImplementation}
export { noOpImplementation }

// Return an object that has functions to send certain HTTP requests to the server.
// All of the routes will have the prefix prepended to them.  e.g. withPathPrefix('localhost:8080')
function withPathPrefix (prefix) {
  return {checkLtlFormula}

  // Return a promise that gets fulfilled if the server responds to our request.
  // The result of the promise: TODO Specify what the promise's result contains
  function checkLtlFormula (petriGameId, formula) {
    return axios.post(prefix + '/checkLtlFormula', {
      petriGameId,
      formula
    })
  }
}

// This is suitable to be used for mocking.  All requests performed with this implementation will
// simply fail immediately.
function noOpImplementation () {
  const handler = {
    get: function (target, name) {
      return function () {
        return Promise.reject(new Error('Route not implemented'))
      }
    }
  }

  return new Proxy({}, handler)
}
