<!--You know Microsoft Paint's tool picker?  It's a grid of icons and you can pick one of them at
 a time to be the active tool.  This component is like that.-->
<template>
  <div class="container"
       :style="`grid-template-rows: repeat(${tools.length}, 20px [col-start])`">
    <div v-for="(tool, index) in tools"
         @click="tool.action !== undefined ? tool.action() : () => {}"
         :style="`grid-column: 1; grid-row-start: ${index}; grid-row-end: ${index + 1}`">
      {{ tool.tool || tool.name }}
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
      }
    },
    data: function () {
      return {
      }
    }
  }
</script>

<style scoped>
  .container {
    display: grid;
  }

</style>
