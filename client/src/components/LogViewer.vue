<template>
  <div class="logViewer" style="display: flex; flex-direction: column;">
    <div style="flex: 1 0 50px; display: flex; justify-content: center; align-items: center;">
      <v-radio-group v-model="logLevel" row hide-details style="padding-top: 0" height="30px">
        <v-radio label="Verbose" :value="1"/>
        <v-radio label="Normal" :value="2"/>
        <v-radio label="Warning" :value="3"/>
        <v-radio label="Error" :value="4"/>
        <v-checkbox label="Show client log" v-model="showClientMessages"/>
        <v-checkbox label="Show server log" v-model="showServerMessages"/>
      </v-radio-group>
    </div>
    <div class="log" ref="logElement" style="flex: 1 1 100%">
      <pre
        v-for="message in visibleMessages"
        :style="styleOfMessage(message)"
        :key="message.uuid"
      >{{ formatMessageDate(message.time) }} {{ message.text }}</pre>
    </div>
  </div>
</template>

<script>
  import {format} from 'date-fns'
  import Vue from 'vue'
  import logging from '../logging'
  const uuidv4 = require('uuid/v4')

  export default {
    name: 'log-viewer',
    props: {},
    data () {
      return {
        logLevel: 2,
        showClientMessages: true,
        showServerMessages: true,
        messageLog: []
      }
    },
    computed: {
      visibleMessages: function () {
        return this.messageLog.filter(m => {
          const isSourceVisible = (m.source === 'client' && this.showClientMessages) ||
            (m.source === 'server' && this.showServerMessages)
          const isLogLevelVisible = m.level >= this.logLevel
          return isLogLevelVisible && isSourceVisible
        })
      }

    },
    created: function () {
      // Subscribe to logging event bus
      logging.subscribeLog(message => {
        this.messageLog.push({
            ...message,
            uuid: uuidv4()
          })
      })
    },
    mounted: function () {
      this.scrollToBottom()
    },
    methods: {
      styleOfMessage: function (message) {
        const colorsOfSources = {
          server: '#007700',
          client: '#003377'
        }
        return `color: ${colorsOfSources[message.source]}`
      },
      formatMessageDate: function (date) {
        return format(date, 'YYYY-MM-DD HH:mm:ss')
      },
      scrollToBottom: function () {
        // We must wait until Vue updates the DOM in order to scroll to the true bottom of the log.
        Vue.nextTick(() => {
          const div = this.$refs.logElement
          div.scrollTop = div.scrollHeight
        })
      }
    },
    watch: {
      visibleMessages: function () {
        this.scrollToBottom()
      }
    }
  }
</script>

<style scoped>
  .logViewer {
    overflow: hidden;
  }

  .log {
    display: block;
    width: 100%;
    overflow: scroll;
  }

  .log pre {
    margin-bottom: 0;
    font-size: 18px;
  }

</style>
