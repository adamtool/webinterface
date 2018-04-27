// Create an idiomatic vue-like wrapper around a websocket so you can write
// socket.$on("message", ...) and
// socket.$on("error", ...)
import Vue from 'vue'
function makeSocket (socketurl) {
  const socket = new WebSocket(socketurl)
  const emitter = new Vue({
    methods: {
      send (message) {
        if (socket.readyState === 1) { socket.send(message) }
      }
    }
  })
  socket.onmessage = function (msg) {
    emitter.$emit('message', msg.data)
  }
  socket.onerror = function (err) {
    emitter.$emit('error', err)
  }

  return emitter
}

export default makeSocket
