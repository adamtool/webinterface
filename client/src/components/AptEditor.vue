<template>
  <div :style="aptEditorStyle">
    <div style="text-align: center; flex: 0 0 58px; line-height: 58px; font-size: 18pt;">
      APT Editor
    </div>
    <div class='apt-input-field'
         contenteditable
         style="flex: 1 1 0; white-space: pre-wrap; overflow: scroll; min-height: 0;"
         @input="onAptInput"
         ref="theInputField"/>
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
        apt: '',
        // Stack of previous states.  [[aptA, restoreCaretA()], [aptB, restoreCaretB()], ...]
        undoHistory: [],
        redoHistory: []
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
    mounted: function () {
      this.$refs.theInputField.addEventListener('keydown', (event) => {
        if (typeof event.which === 'number' && event.which > 0 && !event.ctrlKey) {
          this.undoHistory.push(this.currentHistoryPoint())
          this.redoHistory = []
        }
        if (event.key === 'Return' || event.key === 'Enter') {
          // Insert only newlines rather than <br> or <div> elements when pressing enter
          document.execCommand('insertHTML', false, '\n')
          event.preventDefault()
        }
        if (event.ctrlKey && event.key === 'z') {
          this.onUndo()
          event.preventDefault()
        }
        if (event.ctrlKey && event.key === 'y') {
          this.onRedo()
          event.preventDefault()
        }
      })
    },
    methods: {
      onUndo: function () {
        if (this.undoHistory.length === 0) {
          console.log('undo stack empty')
          return
        }
        console.log('undo')
        this.redoHistory.push(this.currentHistoryPoint())
        const historyPoint = this.undoHistory.pop()
        this.restoreHistoryPoint(historyPoint)
      },
      onRedo: function () {
        if (this.redoHistory.length === 0) {
          console.log('redo stack empty')
          return
        }
        console.log('redo')
        this.undoHistory.push(this.currentHistoryPoint())
        const historyPoint = this.redoHistory.pop()
        this.restoreHistoryPoint(historyPoint)
      },
      restoreHistoryPoint: function ([apt, restoreCaret]) {
        this.apt = apt
        this.$refs.theInputField.innerHTML = this.formatAptWithHighlightedError(this.apt)
        this.$emit('input', this.getPureAptFromInputField())
        restoreCaret()
      },
      currentHistoryPoint: function () {
        return [this.apt, this.saveCaretPosition(this.$refs.theInputField)]
      },
      onAptInput: function () {
        this.apt = this.getPureAptFromInputField()
        this.$emit('input', this.getPureAptFromInputField())
      },
      // Our editor is full of HTML, and we want to clean it up and just get the plain text inside.
      getPureAptFromInputField: function () {
        // Decode escaped strings
        const unescapedText = this.htmlDecode(this.$refs.theInputField.innerHTML)
        // Clean up highlighting annotations
        const textWithoutHighlights = unescapedText.replace(`<span style='color: red;'>`, '')
        const textWithoutHighlights2 = textWithoutHighlights.replace(`</span>`, '')
        return textWithoutHighlights2
      },
      // Un-escape innerHTML strings so that escape strings get converted to the actual
      // characters they represent (e.g. '&lt;' gets converted to '<') to be sent to the server.
      // See https://stackoverflow.com/a/34064434
      htmlDecode: function (input) {
        const doc = new DOMParser().parseFromString(input, 'text/html')
        return doc.documentElement.textContent
      },
      // Return an innerHTML for the apt editor that has the appropriate line/column highlighted
      formatAptWithHighlightedError: function (aptString) {
        if (this.aptParseStatus !== 'error' || !this.isParseErrorHighlightingInfoPresent) {
          return aptString
        }
        const aptLines = aptString.split('\n')
        aptLines[this.aptParseErrorLineNumber - 1] =
          `<span style='color: red;'>${aptLines[this.aptParseErrorLineNumber - 1]}</span>`
        return aptLines.join('\n')
      },
      // Save the position of the cursor (caret) in the text field so it can later be restored
      // We have to do this because we edit the innerHTML in order to provide highlighting,
      // and that loses the caret position.
      // See https://stackoverflow.com/a/38479462
      saveCaretPosition: function (context) {
        var selection = window.getSelection()
        if (!selection || selection.rangeCount === 0) {
          return function restore () {
          } // There is no caret position to restore
        }
        var range = selection.getRangeAt(0).cloneRange()
        range.setStart(context, 0)
        var len = range.toString().length

        return function restore () {
          var pos = getTextNodeAtPosition(context, len)
          selection.removeAllRanges()
          var range = new Range()
          range.setStart(pos.node, pos.position)
          selection.addRange(range)
        }

        function getTextNodeAtPosition (root, index) {
          var treeWalker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT, function next (elem) {
            if (index >= elem.textContent.length) {
              index -= elem.textContent.length
              return NodeFilter.FILTER_REJECT
            }
            return NodeFilter.FILTER_ACCEPT
          })
          var c = treeWalker.nextNode()
          return {
            node: c ? c : root,
            position: c ? index : 0
          }
        }
      }
    },
    watch: {
      undoHistory: function () {
        console.log(`Undo history depth: ${this.undoHistory.length}`)
        console.log(`Redo history depth: ${this.redoHistory.length}`)
      },
      redoHistory: function () {
        console.log(`Undo history depth: ${this.undoHistory.length}`)
        console.log(`Redo history depth: ${this.redoHistory.length}`)
      },
      aptFromAdamParser: function (newApt) {
        if (newApt !== this.apt) {
          this.undoHistory.push(this.currentHistoryPoint())
          this.redoHistory = []
          this.apt = newApt
        }
      },
      // When there's a parse error, highlight the corresponding line of text in the APT editor
      aptParseStatus: function (status) {
        const restore = this.saveCaretPosition(this.$refs.theInputField)
        this.$refs.theInputField.innerHTML = this.formatAptWithHighlightedError(this.apt)
        restore()
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
