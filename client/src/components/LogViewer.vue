<template>
  <div class="logViewer">
    <v-container fluid>
      <v-layout row>
        <v-radio-group v-model="logLevel" row hide-details style="padding-top: 0">
          <v-flex xs6 md2>
            <v-radio label="Normal" :value="2"/>
          </v-flex>
          <v-flex xs6 md2>
            <v-radio label="Verbose" :value="1"/>
          </v-flex>
          <v-flex xs6 md2>
            <v-checkbox label="Show client log" v-model="showClientMessages"/>
          </v-flex>
          <v-flex xs6 md2>
            <v-checkbox label="Show server log" v-model="showServerMessages"/>
          </v-flex>
        </v-radio-group>
      </v-layout>
      <v-layout row>
        <div class="log" ref="logElement">
          <template v-for="message in visibleMessages">
            <pre :style="styleOfMessage(message)">{{ formatMessageDate(message.time) }} {{ message.text }}</pre>
          </template>
        </div>
      </v-layout>
    </v-container>
  </div>

</template>

<script>
  import { format } from 'date-fns'
  import Vue from 'vue'

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
        logLevel: 2,
        showClientMessages: true,
        showServerMessages: true
      }
    },
    computed: {
      visibleMessages: function () {
        return this.messages.filter(m => {
          const isSourceVisible = (m.source === 'client' && this.showClientMessages) ||
            (m.source === 'server' && this.showServerMessages)
          const isLogLevelVisible = m.level >= this.logLevel
          return isLogLevelVisible && isSourceVisible
        })
      }

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
      messages: function () {
        this.scrollToBottom()
      }
    }
  }
</script>

<style scoped>
  .logViewer {
    /*display: block;*/
    height: 350px;
  }

  .log {
    display: block;
    height: 300px;
    width: 100%;
    overflow: scroll;
  }

  .log pre {
    margin-bottom: 0;
  }

</style>
