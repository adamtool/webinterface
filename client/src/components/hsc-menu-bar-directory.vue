// Display a nested file/folder structure in a menu.
// It works using the data structure exported by ../aptExamples.js
<template>
  <div v-if="isRoot">
    <hsc-menu-bar-directory v-for="child in fileTreeNode.children"
                            :key="child.name"
                            :fileTreeNode="child"
                            :callback="callback"
                            :isRoot="false"/>
  </div>
  <hsc-menu-item v-else-if="fileTreeNode.type === 'file'"
                 :key="fileTreeNode.name"
                 :label="fileTreeNode.name"
                 @click="callback(fileTreeNode.body)"/>
  <hsc-menu-item v-else-if="fileTreeNode.type === 'directory'"
                 :label="fileTreeNode.name">
    <hsc-menu-bar-directory v-for="child in fileTreeNode.children"
                            :key="child.name"
                            :fileTreeNode="child"
                            :callback="callback"
                            :isRoot="false"/>
  </hsc-menu-item>
  <div v-else>
    <hsc-menu-item label="Something went wrong"
                   @click="callback('File did not load')">
      {{ fileTreeNode }}
    </hsc-menu-item>
  </div>
</template>

<script>
  import Vue from 'vue'
  import * as VueMenu from '@hscmap/vue-menu'

  Vue.use(VueMenu)
  import MyVueMenuTheme from '../menuStyle'

  export default {
    name: 'hsc-menu-bar-directory',
    components: {
      'my-theme': MyVueMenuTheme
    },
    props: {
      fileTreeNode: {
        type: Object,
        required: true
      },
      callback: {
        type: Function,
        required: true
      },
      isRoot: {
        type: Boolean,
        default: true
      }
    }
  }
</script>

<style scoped>

</style>
