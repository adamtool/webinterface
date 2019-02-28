<template>
  <div :style="aptEditorStyle">
    <div style="text-align: center; flex: 0 0 58px; line-height: 58px; font-size: 18pt;">
      APT Editor
    </div>
    <textarea class='apt-text-area' style="flex: 1 1 100%" v-model='aptInTextField' @input="emitAptChanged"/>
    <div style="color: red;">{{ aptParseError }} {{ lineAndColumnText }}
    </div>
  </div>
</template>

<script>
  import logging from '../logging'

  export default {
    name: 'AptEditor',
    data: function () {
      return {
        aptInTextField: ''
      }
    },
    props: {
      aptFromAdamParser: {
        type: String,
        required: true
      },
      aptParseStatus: {
        type: String,
        required: true,
        validator: function (value) {
          // The value must match one of these strings
          return ['success', 'error', 'running'].indexOf(value) !== -1
        }
      },
      aptParseError: {
        type: String,
        required: true
      },
      aptParseErrorLineNumber: {
        type: Number,
        required: true
      },
      aptParseErrorColumnNumber: {
        type: Number,
        required: true
      }
    },
    methods: {
      emitAptChanged: function () {
        this.$emit('input', this.aptInTextField)
      }
    },
    watch: {
      aptFromAdamParser: function () {
        this.aptInTextField = this.aptFromAdamParser
      }
    },
    computed: {
      lineAndColumnText: function () {
        if (this.aptParseErrorLineNumber === -1 && this.aptParseErrorColumnNumber === -1) {
          return ''
        }
        return `(line ${this.aptParseErrorLineNumber}, column ${this.aptParseErrorColumnNumber})`
      },
      aptEditorStyle: function () {
        let color
        switch (this.aptParseStatus) {
          case 'success':
            color = '#5959ed'
            break
          case 'error':
            color = 'red'
            break
          case 'running':
            color = 'lightgray'
            break
          default:
            logging.sendErrorNotification('Got an invalid value for aptParseStatus: ' + this.aptParseStatus)
        }
        const borderStyle = `border: 3px solid ${color};`
        const layoutStyle = 'display: flex; flex-direction: column; height: 100%;'
        return layoutStyle + borderStyle
      }
    }
  }
</script>

<style scoped>
  .apt-text-area {
    background: white;
    box-sizing: border-box;
    width: 100%;
    padding-left: 10px;
    resize: none;
    font-size: 18px;
  }
</style>
