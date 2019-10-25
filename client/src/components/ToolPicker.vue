<!--You know Microsoft Paint's tool picker?  It's a grid of icons and you can pick one of them at
 a time to be the active tool.  This component is like that.-->
<template>
  <div class="toolpicker-root-el"
       ref="container"
       :style="`grid-template-rows: repeat(${visibleTools.length}, auto [col-start])`"
       @mouseover="hover = true"
       @mouseleave="hover = false"
  >
    <template v-for="(tool, index) in visibleTools">
      <!--Normal rows in the grid-->
      <div v-if="tool.type === 'tool' || tool.type === 'action'"
           @click="onClick(tool)"
           :class="selectedTool === tool ? 'toolbar-row tool selected-tool' : 'toolbar-row tool'"
           :style="`grid-column: 1; grid-row-start: ${index}; grid-row-end: ${index + 1}`"
           v-ripple
      >
        <v-btn
          small
          :ripple="false"
          icon>
          <v-icon
            v-if="tool.icon">
            {{ tool.icon }}
          </v-icon>
        </v-btn>
        <div class="tool-name">
          {{ tool.name }}
        </div>
      </div>
      <!--Dividers-->
      <div
        v-else-if="tool.type === 'divider'"
        class="divider toolbar-row"
        :style="`grid-column: 1; grid-row-start: ${index}; grid-row-end: ${index + 1}`"
      >
        <v-divider/>
      </div>
      <!--Ellipsis to show overflow on small screens-->
      <v-menu
        v-else-if="tool.type === 'ellipsis'"
        v-model="isOverflowMenuOpen"
      >
        <template v-slot:activator="{ on }"
        >
          <div
            class="divider toolbar-row tool"
            :style="`grid-column: 1; grid-row-start: ${index}; grid-row-end: ${index + 1}`"
            v-on="on"
          >
            <v-btn
              small
              icon
              :ripple="false"
              v-on="$parent.on"
            >
              <v-icon>more_horiz</v-icon>
            </v-btn>
            <div
              class="tool-name"
              v-on="$parent.on"
            >
              More
            </div>
          </div>
        </template>
        <v-list
          dense
        >
          <template
            v-for="(hiddenItem, index) in tool.hiddenItems"
          >
            <v-list-item
              v-if="hiddenItem.type === 'tool' || hiddenItem.type === 'action'"
              @click="onClick(hiddenItem)"
              :key="index"
            >
              <v-list-item-content>
                  <span>
                  <v-btn
                    small
                    icon>
                    <v-icon
                      v-if="hiddenItem.icon">
                      {{ hiddenItem.icon }}
                    </v-icon>
                  </v-btn>
                  <span class="tool-name">
                    {{ hiddenItem.name }}
                  </span>
                  </span>
              </v-list-item-content>
            </v-list-item>
            <v-divider
              v-else-if="hiddenItem.type === 'divider'"
              :key="index"
            />
            <div
              v-else
              :key="index"
            >
              Error: Unknown item type: {{ hiddenItem.type }}
            </div>
          </template>
        </v-list>
      </v-menu>
      <template v-else>
        <v-icon>error</v-icon>
      </template>
    </template>
  </div>

</template>

<script>
  const ResizeSensor = require('css-element-queries/src/ResizeSensor')
  import Vue from 'vue'
  import Vuetify from 'vuetify'

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
      },
      // How many pixels should be left, top and bottom, between the edges of the tool picker and
      // the top/bottom edges of the parent element
      paddingWithinParentElement: {
        type: Number,
        required: true
      }
    },
    data: function () {
      return {
        collapse: false,
        hover: false,
        isOverflowMenuOpen: false,
        reactiveParentHeight: 0
      }
    },
    mounted: function () {
      this.reactiveParentHeight = this.$refs.container.parentElement.clientHeight

      this.onContainerResize = () => {
        if (this.$refs.container.parentElement.clientHeight > 0) {
          this.reactiveParentHeight = this.$refs.container.parentElement.clientHeight
        }
      }
      new ResizeSensor(this.$refs.container.parentElement, this.onContainerResize)
      window.addEventListener('resize', this.onContainerResize)
    },
    destroy: function () {
      window.removeEventListener('resize', this.onContainerResize)
    },
    computed: {
      shouldActCollapsed: function () {
        return this.collapse && !this.hover && !this.isOverflowMenuOpen
      },
      visibleTools: function () {
        if (this.shouldActCollapsed && this.selectedTool.visible) {
          return [
            this.collapseButton,
            {type: 'divider'},
            this.selectedTool
          ]
        } else if (this.shouldActCollapsed && !this.selectedTool.visible) {
          return [
            this.collapseButton
          ]
        } else {
          return this.withResponsiveEllipsis([
            this.collapseButton,
            {type: 'divider'},
            ...this.tools.filter(tool => tool.visible !== false)
          ])
        }
      },
      collapseButton: function () {
        return {
          type: 'action',
          action: () => this.collapse = !this.collapse,
          icon: this.collapse ? 'visibility' : 'visibility_off',
          name: this.collapse ? 'Always show' : 'Collapse'
        }
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
      // Truncate menuItems with an ellipsis in case there are too many to fit on screen
      withResponsiveEllipsis: function (menuItems) {
        let availableHeight = this.reactiveParentHeight - this.paddingWithinParentElement
        let lastItemIndex = 0
        menuItems.forEach(menuItem => {
          const heightNeeded = heightOfItem(menuItem)
          if (availableHeight < heightNeeded) {
            return
          }
          lastItemIndex = lastItemIndex + 1
          availableHeight = availableHeight - heightNeeded
        })

        if (lastItemIndex === menuItems.length) {
          return menuItems
        }

        const visibleItems = menuItems.slice(0, lastItemIndex)
        const hiddenItems = menuItems.slice(lastItemIndex)

        return [
          ...visibleItems,
          {
            type: 'ellipsis',
            hiddenItems
          }
        ]

        function heightOfItem (item) {
          switch (item.type) {
            case 'tool':
            case 'action':
              return 40
            case 'divider':
              return 13
            default:
              throw new Error('Unrecognized item type: ' + item.type)
          }
        }
      },
      onClick: function (tool) {
        switch (tool.type) {
          case 'action':
            tool.action()
            break
          case 'tool':
            this.$emit('onPickTool', tool)
            break
          case 'divider':
          case 'ellipsis':
            break
          default:
            throw new Error(`Unrecognized tool type '${tool.type}' for tool named '${tool.name}'`)
        }
      }
    }
  }
</script>

<style scoped>
  .toolpicker-root-el {
    display: grid;
    width: auto;
  }

  .divider {
    margin-top: 6px;
    margin-bottom: 6px;
  }

  .toolbar-row {
    border: 1px;
    padding-left: 4px;
    padding-right: 4px;
  }

  .toolbar-row.selected-tool {
    border: 1px solid #555555;
    border-radius: 10px;
  }

  .toolbar-row.selected-tool:hover {
    border: 1px solid #000000;
  }


  /*Disable hover effect on v-btn */
  .toolbar-row .v-btn::before {
    opacity: 0;
  }

  .toolbar-row .v-btn {
    margin: 6px;
  }

  .tool:hover {
    border-radius: 10px;
    background-color: rgba(0, 0, 0, 0.15);
  }

  .toolpicker-root-el > .toolbar-row > .tool-name {
    display: none;
  }

  .toolpicker-root-el:hover > .toolbar-row > .tool-name {
    display: inline-block;
  }

</style>
