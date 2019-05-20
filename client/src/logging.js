// This module sets up a global event bus that can be shared by all components.
// The idea is that, in any component, you can import log, logVerbose, logError, etc.
// and then, in a component where you want to display the log, you use the method subscribeLog()
// to get ahold of all of the corresponding events.
import Vue from 'vue'

export default {
  subscribeLog,
  unsubscribeLog,
  log,
  logObject,
  logVerbose,
  logError,
  logServerMessage,
  sendErrorNotification,
  sendSuccessNotification,
  resetNotification,
  subscribeErrorNotification,
  subscribeSuccessNotification,
  subscribeResetNotification,
  unsubscribeErrorNotification,
  unsubscribeSuccessNotification,
  unsubscribeResetNotification
}

const EventBus = new Vue()

function subscribeLog (callback) {
  EventBus.$on('logMessage', callback)
}

function unsubscribeLog (callback) {
  EventBus.$off('logMessage', callback)
}

function subscribeErrorNotification (callback) {
  EventBus.$on('errorNotification', callback)
}

function subscribeSuccessNotification (callback) {
  EventBus.$on('successNotification', callback)
}

function subscribeResetNotification (callback) {
  EventBus.$on('resetNotification', callback)
}

function unsubscribeErrorNotification (callback) {
  EventBus.$off('errorNotification', callback)
}

function unsubscribeSuccessNotification (callback) {
  EventBus.$off('successNotification', callback)
}

function unsubscribeResetNotification (callback) {
  EventBus.$off('resetNotification', callback)
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
  console.log(message)
  log(message, 0)
}

function logVerbose (message) {
  log(message, 1)
}

function logError (message) {
  log(message, 4)
}

function sendErrorNotification (message) {
  logError(message)
  EventBus.$emit('errorNotification', message)
}

function sendSuccessNotification (message) {
  log(message)
  EventBus.$emit('successNotification', message)
}

// This is intended to e.g. trigger clearing the message from the transient notification area
function resetNotification () {
  EventBus.$emit('resetNotification')
}
