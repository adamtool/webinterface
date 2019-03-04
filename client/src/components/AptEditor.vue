<template>
  <div :style="aptEditorStyle">
    <div style="text-align: center; flex: 0 0 58px; line-height: 58px; font-size: 18pt;">
      APT Editor
    </div>
    <div class='apt-text-area'
         contenteditable
         style="flex: 1 1 100%; white-space: pre-wrap;"
         @input="emitAptChanged"
         ref="theTextArea"/>
    <div style="color: red;">{{ aptParseError }}
    </div>
  </div>
</template>

<script>
  import logging from '../logging'

  export default {
    name: 'AptEditor',
    data: function () {
      return {
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
        this.$emit('input', this.$refs.theTextArea.innerHTML)
      }
    },
    watch: {
      aptFromAdamParser: function () {
        this.$refs.theTextArea.innerHTML = this.aptFromAdamParser
      },
      // When there's a parse error, highlight the corresponding line of text in the APT editor
      aptParseStatus: function (status) {
        if (status === 'error' && this.isParseErrorHighlightingInfoPresent) {
          // TODO Implement
          // this.selectTextAreaLine(this.$refs.theTextArea, this.aptParseErrorLineNumber)
        }
      }
    },
    computed: {
      isParseErrorHighlightingInfoPresent: function () {
        const isLineNumberValid = this.aptParseErrorLineNumber !== -1 && this.aptParseErrorLineNumber !== undefined
        const isColumnNumberValid = this.aptParseErrorColumnNumber !== -1 && this.aptParseErrorColumnNumber !== undefined
        return isLineNumberValid && isColumnNumberValid
      },
      lineAndColumnText: function () {
        if (!this.isParseErrorHighlightingInfoPresent) {
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
