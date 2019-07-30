<template>
  <div :style="aptEditorStyle">
    <div style="text-align: center; flex: 0 0 58px; line-height: 58px; font-size: 18pt;">
      APT Editor
    </div>
    <textarea class='apt-input-field'
              style="white-space: pre-wrap; overflow: scroll; min-height: 0;"
              @input="onAptInput"
              v-model="apt"
              ref="theTextArea"/>
    <div style="color: red;">{{ aptParseError }}
    </div>
  </div>
</template>

<script>
  import logging from '../logging'
  import {HighlightWithinTextarea} from 'highlight-within-textarea-unjquery'

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
    height: 100%;
    background: white;
    box-sizing: border-box;
    width: 100%;
    padding-left: 10px;
    resize: none;
    font-size: 18px;
  }
</style>

<!--Global, unscoped styles necessary for the library highlight-within-textfield to work-->
<style>
  .hwt-container {
    flex: 1 1 0;
    display: inline-block;
    position: relative;
    overflow: hidden !important;
    -webkit-text-size-adjust: none !important;
  }

  .hwt-backdrop {
    position: absolute !important;
    top: 0 !important;
    right: -99px !important;
    bottom: 0 !important;
    left: 0 !important;
    padding-right: 99px !important;
    overflow-x: hidden !important;
    overflow-y: auto !important;
  }

  .hwt-highlights {
    width: auto !important;
    height: auto !important;
    border-color: transparent !important;
    white-space: pre-wrap !important;
    word-wrap: break-word !important;
    color: transparent !important;
    overflow: hidden !important;
  }

  .hwt-input {
    display: block !important;
    position: relative !important;
    margin: 0;
    padding: 0;
    border-radius: 0;
    font: inherit;
    overflow-x: hidden !important;
    overflow-y: auto !important;
  }

  .hwt-content {
    border: 1px solid;
    background: none transparent !important;
  }

  .hwt-content mark {
    padding: 0 !important;
    color: inherit;
  }
</style>
