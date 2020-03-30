<!--This component should 'extend' the graph editor component and modify its template a little
 bit.  There is a lot of debate within the community as to what the best way is to do this in Vue,
 because Vue 2 at the moment does not have any built in form of 'template inheritance'.
 I have settled for now on the solution suggested by "ThaDaVos" in this github issue:
 https://github.com/vuejs/vue/issues/6811#issuecomment-401049094

 Here are a couple of articles detailing other possible approaches to extending/inheriting
 components.
 A broad overview:
 https://vuejsdevelopers.com/2017/06/11/vue-js-extending-components/
 A how-to using the 'pug' HTML preprocessor:
 https://vuejsdevelopers.com/2020/02/24/extending-vuejs-components-templates/
 -->
<template>
  <GraphEditor
    v-bind="$props"
    ref="graphEditor"
  />
</template>

<script>
  import GraphEditor from './GraphEditor'

  export default {
    name: 'Simulator',
    components: {GraphEditor},
    props: {
      ...GraphEditor.props
    }
  }
</script>
