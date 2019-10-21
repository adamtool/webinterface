<!--You know Microsoft Paint's tool picker?  It's a grid of icons and you can pick one of them at
 a time to be the active tool.  This component is like that.-->
<template>
  <div class="toolpicker-root-el"
       ref="container"
       :style="`grid-template-rows: repeat(${visibleTools.length}, auto [col-start])`"
       @mouseover="hover = true"
       @mouseleave="hover = false"
  >
    <div v-for="(tool, index) in visibleTools"
         @click="onClick(tool)"
         :class="classOfToolRow(tool)"
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
      <!--Dividers-->
      <v-divider
        v-else-if="tool.type === 'divider'"
        class="divider"
      />
      <!--Ellipsis to show overflow on small screens-->
      <template v-else-if="tool.type === 'ellipsis'">
        <v-menu
          v-model="isOverflowMenuOpen"
        >
          <template v-slot:activator="{ on }">
            <v-btn
              small
              icon
              v-on="on"
            >
              <v-icon>more_horiz</v-icon>
            </v-btn>
            <div class="tool-name">
              More
            </div>
          </template>
          <v-list
            dense
          >
            <template
              v-for="(hiddenItem, index) in tool.hiddenItems"
            >
              <v-list-tile
                v-if="hiddenItem.type === 'tool' || hiddenItem.type === 'action'"
                @click="onClick(hiddenItem)"
                :key="index"
              >
                <v-list-tile-content>
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
                </v-list-tile-content>
              </v-list-tile>
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
      </template>
      <template v-else>
        <v-icon>error</v-icon>
      </template>
    </div>
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
      classOfToolRow: function (tool) {
        if (tool.type === 'divider') {
          return 'toolbar-row'
        }
        return this.selectedTool === tool ? 'toolbar-row tool selected-tool' : 'toolbar-row tool'
      },
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
    border: 1px solid #aaaaaa;
    border-radius: 10px;
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
