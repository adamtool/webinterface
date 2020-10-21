<template>
  <div style="">
    <v-expansion-panels multiple
                        v-model="openPanels"
    >
      <v-expansion-panel value="true">
        <v-expansion-panel-header>
          Keyboard shortcuts
        </v-expansion-panel-header>
        <v-expansion-panel-content>
          <ul>
            <li>
              <strong>Shift + click and drag:</strong> Pan the view of the whole graph.
            </li>
            <li>
              <strong>Scroll wheel:</strong> Zoom in and zoom out.
            </li>
            <li>
              <strong>Delete:</strong> Delete the selected nodes.
            </li>
            <li>
              <strong>Hold Ctrl while clicking and dragging a node:</strong> Snap node to grid.
            </li>
          </ul>
        </v-expansion-panel-content>
      </v-expansion-panel>
      <v-expansion-panel v-if="useModelChecking">
        <v-expansion-panel-header>
          MC Syntax
        </v-expansion-panel-header>
        <v-expansion-panel-content>
          <p>
            Placeholder text
          </p>
        </v-expansion-panel-content>
      </v-expansion-panel>
    </v-expansion-panels>
  </div>
</template>

<script>
  export default {
    name: 'Help',
    data: function () {
      return {
        openPanels: [0, 1]
      }
    },
    props: {
      useModelChecking: {
        type: Boolean,
        required: true
      }
    }
  }
</script>

<style scoped>

</style>
