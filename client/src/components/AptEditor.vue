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
        type: Number,
        required: true
      },
      aptParseErrorColumnNumber: {
        type: Number,
        required: true
      }
    },
    mounted: function () {
      // Insert only newlines rather than <br> or <div> elements when pressing enter
      this.$refs.theTextArea.addEventListener('keydown', function (event) {
        if (event.keyCode === 13) {
          document.execCommand('insertHTML', false, '\n')
          event.preventDefault()
        }
      })
    },
    methods: {
      emitAptChanged: function () {
        this.apt = this.getPureAptFromTextArea()
        console.log(this.apt)
        this.$emit('input', this.getPureAptFromTextArea())
      },
      getPureAptFromTextArea: function () {
        // Decode escaped strings
        const a = this.$refs.theTextArea.innerHTML
        const b = a.replace('<br>', '\n')
        const unescapedText = this.htmlDecode(b)
        // Clean up highlighting annotations
        const textWithoutHighlights = unescapedText.replace(`<span style='color: red;'>`, '')
        const textWithoutHighlights2 = textWithoutHighlights.replace(`</span>`, '')
        return textWithoutHighlights2
      },
      // Un-escape innerHTML strings so that e.g. proper angle brackets ('<') get sent to server
      // rather than escape strings like &lt;
      // See https://stackoverflow.com/a/34064434
      htmlDecode: function (input) {
        const doc = new DOMParser().parseFromString(input, 'text/html')
        return doc.documentElement.textContent
      },
      // Return a innerHTML for the apt editor that has the appropriate line/column highlighted
      formatAptWithHighlightedError: function (aptString) {
        if (this.aptParseStatus !== 'error' || !this.isParseErrorHighlightingInfoPresent) {
          return aptString
        }
        const aptLines = aptString.split('\n')
        aptLines[this.aptParseErrorLineNumber - 1] =
          `<span style='color: red;'>${aptLines[this.aptParseErrorLineNumber - 1]}</span>`
        return aptLines.join('\n')
      },
      // See https://stackoverflow.com/a/38479462
      saveCaretPosition: function (context) {
        var selection = window.getSelection()
        if (!selection) {
          return function restore () {} // There is no caret position to restore
        }
        var range = selection.getRangeAt(0)
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
          // var lastNode = null

          var treeWalker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT, function next (elem) {
            if (index >= elem.textContent.length) {
              index -= elem.textContent.length
              // lastNode = elem
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
      aptFromAdamParser: function () {
        this.apt = this.aptFromAdamParser
        // const restore = this.saveCaretPosition(this.$refs.theTextArea)
        // this.$refs.theTextArea.innerHTML =
        //   this.formatAptWithHighlightedError(this.aptFromAdamParser)
        // restore()
      },
      apt: function () {
      },
      // When there's a parse error, highlight the corresponding line of text in the APT editor
      aptParseStatus: function (status) {
        const restore = this.saveCaretPosition(this.$refs.theTextArea)
        this.$refs.theTextArea.innerHTML = this.formatAptWithHighlightedError(this.apt)
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
  .apt-text-area {
    background: white;
    box-sizing: border-box;
    width: 100%;
    padding-left: 10px;
    resize: none;
    font-size: 18px;
  }
</style>
