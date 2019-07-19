// Usage:
// import * as mcroutes from 'modelCheckingRoutes'
// const modelCheckingRoutes = mcroutes.withPathPrefix('localhost:8080')
// [...]
// modelCheckingRoutes.parseLtlFormula(someId, someFormula)

import * as axios from 'axios'

export { noOpImplementation, withPathPrefix }

// Return an object that has functions to send certain HTTP requests to the server.
// All of the routes will have the prefix prepended to them.  e.g. withPathPrefix('localhost:8080')
function withPathPrefix (prefix) {
  return {parseLtlFormula}

  // Return a promise that gets fulfilled if the server responds to our request.
  // The result of the promise: TODO Specify what the promise's result contains
  async function parseLtlFormula (petriGameId, formula) {
    return axios.post(prefix + '/parseLtlFormula', {
      petriGameId,
      formula
    })
  }
}

// This is suitable to be used for mocking.  All requests performed with this implementation will
// simply fail after 500 milliseconds.
function noOpImplementation () {
  const handler = {
    get: function (target, name) {
      return async function () {
        await sleep(500)
        return Promise.reject(new Error('Route not implemented (this is a mock-up)'))
      }
    }
  }

  return new Proxy({}, handler)

  async function sleep (ms) {
    await new Promise(resolve => setTimeout(resolve, ms))
  }
}
