<!--You know Microsoft Paint's tool picker?  It's a grid of icons and you can pick one of them at
 a time to be the active tool.  This component is like that.-->
<template>
  <div class="container"
       :style="`grid-template-rows: repeat(${visibleTools.length}, auto [col-start])`">
    <div v-for="(tool, index) in visibleTools"
         @click="onClick(tool)"
         :class="selectedTool === tool ? 'toolbar-row selected-tool' : 'toolbar-row'"
         :style="`grid-column: 1; grid-row-start: ${index}; grid-row-end: ${index + 1}`">
      <!--Normal rows in the grid-->
      <template v-if="tool.type === 'tool' || tool.type === 'action'">
        <v-btn
          small
          icon>
          <v-icon
            v-if="tool.icon">
            {{ tool.icon }}
          </v-icon>
        </v-btn>
        <div class="tool-name">
          {{ tool.name }}
        </div>
      </template>
      <!--Separators-->
      <template v-else>
        <div style="background: black; height: 5px;"></div>
      </template>

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
      // TODO figure out initialization in App so that this validator can be used
      selectedTool: {
        // type: Object,
        // required: true,
        // validator: tool => tools.includes(tool) && tool.type === 'tool'
      }
    },
    data: function () {
      return {}
    },
    computed: {
      visibleTools: function () {
        return this.tools.filter(tool => tool.visible !== false)
      }
    },
    watch: {
      selectedTool: function () {
        console.log('selectedTool:')
        console.log(this.selectedTool)
        console.log(`is selectedTool a member of tools? ${this.tools.includes(this.selectedTool)}`)
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
      }
    }
  }
</script>

<style scoped>
  .container {
    display: grid;
    width: auto;
  }

  .selected-tool {
    border: 1px solid lightgray;
  }

  .container > .toolbar-row > .tool-name {
    display: none;
  }

  .container:hover > .toolbar-row > .tool-name {
    display: inline-block;
  }

</style>
