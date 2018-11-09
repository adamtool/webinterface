// This module sets up a global event bus that can be shared by all components.
// The idea is that, in any component, you can import log, logVerbose, logError, etc.
// and then, in a component where you want to display the log, you use the method subscribe()
// to get ahold of all of the corresponding events.
import Vue from 'vue'

export default { subscribe, unsubscribe, log, logObject, logVerbose, logError, logServerMessage }

const EventBus = new Vue()

function subscribe (callback) {
  EventBus.$on('logMessage', callback)
}

function unsubscribe (callback) {
  EventBus.$off('logMessage', callback)
}

function logServerMessage (message, level) {
  EventBus.$emit('logMessage', {
    source: 'server',
    level: level,
    time: new Date(),
    text: message
  })
}

function log (message, level) {
  EventBus.$emit('logMessage', {
    source: 'client',
    level: level === undefined ? 2 : level,
    time: new Date(),
    text: message
  })
}

function logObject (message) {
  log(message, 0)
}

function logVerbose (message) {
  log(message, 1)
}

function logError (message) {
  log(message, 4)
}
