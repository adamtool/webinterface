<template>
  <div class="logViewer">
    <div>Current logging level: {{ logLevel }}</div>
    <template v-for="message in messages.filter(m => m.level >= logLevel)">
      <pre :style="styleOfMessage(message)">{{ message.text }}</pre>
    </template>

  </div>

</template>

<script>
  export default {
    name: 'log-viewer',
    props: {
      messages: {
        type: Array,
        required: true,
        // It might be nice to switch to Typescript to avoid the need for this ad-hoc type checking.
        // This is actually O(n) in the number of log messages, it's really not a great solution.
        validator: messageList => messageList.every(message =>
          message.hasOwnProperty('source') &&
          message.hasOwnProperty('level') &&
          message.hasOwnProperty('time') &&
          message.hasOwnProperty('text')
        )
      }
    },
    data () {
      return {
        logLevel: 2
      }
    },
    methods: {
      styleOfMessage: function (message) {
        const colorsOfSources = {
          server: '#007700',
          client: '#003377'
        }
        return `color: ${colorsOfSources[message.source]}`
      }
    }
  }
</script>

<style scoped>
  .logViewer {
    display: block;
    height: 300px;
    width: 100%;
    overflow: scroll;
  }

</style>
