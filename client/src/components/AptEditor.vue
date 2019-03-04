<template>
  <div :style="aptEditorStyle">
    <div style="text-align: center; flex: 0 0 58px; line-height: 58px; font-size: 18pt;">
      APT Editor
    </div>
    <textarea class='apt-text-area'
              style="flex: 1 1 100%"
              v-model='aptInTextField'
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
      },
      // Select a specific line of text in the text area
      selectTextAreaLine: function (tarea, lineNum) {
        lineNum-- // array starts at 0
        const lines = tarea.value.split('\n')

        // calculate start/end
        let startPos = 0
        let endPos = tarea.value.length
        for (let x = 0; x < lines.length; x++) {
          if (x === lineNum) {
            break
          }
          startPos += (lines[x].length + 1)
        }

        endPos = lines[lineNum].length + startPos

        // do selection
        // Chrome / Firefox

        if (typeof (tarea.selectionStart) !== 'undefined') {
          tarea.focus()
          tarea.selectionStart = startPos
          tarea.selectionEnd = endPos
          return true
        }

        // IE
        if (document.selection && document.selection.createRange) {
          tarea.focus()
          tarea.select()
          const range = document.selection.createRange()
          range.collapse(true)
          range.moveEnd('character', endPos)
          range.moveStart('character', startPos)
          range.select()
          return true
        }
        return false
      }
    },
    watch: {
      aptFromAdamParser: function () {
        this.aptInTextField = this.aptFromAdamParser
      },
      // When there's a parse error, highlight the corresponding line of text in the APT editor
      aptParseStatus: function (status) {
        if (status === 'error' && this.isParseErrorHighlightingInfoPresent) {
          this.selectTextAreaLine(this.$refs.theTextArea, this.aptParseErrorLineNumber)
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
