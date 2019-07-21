<!--You know Microsoft Paint's tool picker?  It's a grid of icons and you can pick one of them at
 a time to be the active tool.  This component is like that.-->
<template>
  <div class="container"
       :style="`grid-template-rows: repeat(${tools.length}, 20px [col-start])`">
    <div v-for="(tool, index) in tools"
         @click="onClick(tool)"
         :class="selectedTool === tool ? 'selected-tool' : ''"
         :style="`grid-column: 1; grid-row-start: ${index}; grid-row-end: ${index + 1}`">
      {{ tool.name }}
    </div>
  </div>

</template>

<script>
  export default {
    name: 'ToolPicker',
    props: {
      tools: {
        type: Array,
        required: true
      },
      // TODO validate and make sure it's part of the 'tools' array
      selectedTool: {
        // type: Object,
        // required: true,
        // validator: tool => tools.includes(tool) && tool.tool !== undefined
      }
    },
    data: function () {
      return {
      }
    },
    methods: {
      onClick: function (tool) {
        switch (tool.type) {
          case 'action':
            tool.action()
            break
          case 'tool':
            this.$emit('onPickTool', tool)
            break
          default:
            throw new Error(`Unrecognized tool type '${tool.type}' for tool named '${tool.name}'`)
        }
        tool.type === 'action' ? tool.action() : () => {}
      }
    }
  }
</script>

<style scoped>
  .container {
    display: grid;
  }

  .selected-tool {
    background-color: '#77FF77'
  }

</style>
