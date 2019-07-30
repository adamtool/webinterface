<template>
  <div :style="aptEditorStyle">
    <div style="text-align: center; flex: 0 0 58px; line-height: 58px; font-size: 18pt;">
      APT Editor
    </div>
    <textarea class='apt-input-field'
              style="flex: 1 1 0; white-space: pre-wrap; overflow: scroll; min-height: 0;"
              @input="onAptInput"
              v-model="apt"
              ref="theTextArea"/>
    <div style="color: red;">{{ aptParseError }}
    </div>
  </div>
</template>

<script>
  import logging from '../logging'
  import { HighlightWithinTextarea } from 'highlight-within-textarea-unjquery'

  export default {
    name: 'AptEditor',
    data: function () {
      return {
        apt: ''
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
        validator: function (value) {
          return typeof value === 'number' || value === undefined
        },
        required: true
      },
      aptParseErrorColumnNumber: {
        validator: function (value) {
          return typeof value === 'number' || value === undefined
        },
        required: true
      }
    },
    created: function () {
      this.apt = this.aptFromAdamParser
    },
    mounted: function () {
      const highlighter = new HighlightWithinTextarea(this.$refs.theTextArea, {
        highlight: [2, 6]
      })
    },
    methods: {
      onAptInput: function () {
        this.$emit('input', this.apt)
      }
    },
    watch: {
      aptFromAdamParser: function (newApt) {
        this.apt = newApt
      },
      // When there's a parse error, highlight the corresponding line of text in the APT editor
      aptParseStatus: function (status) {
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
  .apt-input-field {
    background: white;
    box-sizing: border-box;
    width: 100%;
    padding-left: 10px;
    resize: none;
    font-size: 18px;
  }
</style>
