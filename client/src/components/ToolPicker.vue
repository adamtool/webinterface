<!--You know Microsoft Paint's tool picker?  It's a grid of icons and you can pick one of them at
 a time to be the active tool.  This component is like that.-->
<template>
  <div class="container"
       :style="`grid-template-rows: repeat(${tools.length}, 20px [col-start])`">
    <div v-for="(tool, index) in tools"
         :style="`grid-column: 1; grid-row-start: ${index}; grid-row-end: ${index + 1}`">
      {{ tool.icon || tool.tool }}
    </div>
  </div>

</template>

<script>
  export default {
    name: 'ToolPicker',
    data: function () {
      return {
        tools: [
          {
            icon: 'S',
            tool: 'select'
          },
          {
            icon: 'X',
            tool: 'deleteNodesAndFlows'
          },
          {
            icon: 'D',
            tool: 'drawFlow'
          },
          {
            icon: 'DX',
            tool: 'drawTokenFlow'
          },
          {
            icon: 'SYS',
            tool: 'insertSysPlace'
          },
          {
            icon: 'ENV',
            tool: 'insertEnvPlace'
          },
          {
            icon: 'T',
            tool: 'insertTransition'
          },
          {
            tool: 'autoLayout'
          },
          {
            tool: 'zoomToFitAllNodes'
          },
          {
            tool: 'moveAllNodesToVisibleArea'
          },
          {
            tool: 'freezeAllNodes'
          },
          {
            tool: 'unfreezeAllNodes'
          },
          {
            tool: 'deleteSelectedNodes'
          },
          {
            tool: 'invertSelection'
          },
          {
            tool: 'saveAsSVG'
          }
        ]
      }
    }
  }
</script>

<style scoped>
  .container {
    display: grid;
  }

</style>
