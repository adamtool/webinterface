<template>
  <!--TODO Mark the tabs somehow if the Petri Game has been modified since the tabs were
  opened-->
  <v-tab
    :href="`#tab-${tab.uuid}`"
  >
    <div style="max-width: 150px; ">{{ tabTitle }}</div>
    <!--Spinny circle for running job with X inside -->
    <div
      v-if="tab.jobStatus === 'RUNNING'"
    >
      <v-progress-circular
        indeterminate
        color="blue darken-2"
        :size="26"
        :width="2"
      >
        <v-icon
          small
          style="position: absolute;"
          @click="closeTab">
          close
        </v-icon>
      </v-progress-circular>
    </div>
    <!--Spinny circle for a job that is being canceled-->
    <v-progress-circular
      v-else-if="tab.jobStatus === 'CANCELING'"
      indeterminate
      color="deep-orange"
      :size="26"
      :width="3"
    />
    <!--Show an X to close the tab/cancel the running job-->
    <v-icon
      v-else-if="tab.isCloseable"
      small right
      @click="closeTab">
      close
    </v-icon>
    <template
      v-else/>
  </v-tab>
</template>

<script>
  import {formatJobType} from '../jobType'
  export default {
    name: 'JobTab.vue',
    props: {
      tab: {
        type: Object,
        required: true
      }
    },
    methods: {
      closeTab: function () {
        this.$emit('closeTab')
      },
    },
    computed: {
      tabTitle: function () {
        if (this.tab.type === 'errorMessage') {
          return 'Error'
        }
        const typePrettyPrinted = formatJobType(this.tab.type)
        const shouldIncludeFormula =
          ['MODEL_CHECKING_NET', 'MODEL_CHECKING_RESULT'].includes(this.tab.type)
        const formulaText = shouldIncludeFormula ?
          ` for "${this.tab.jobKey.requestParams.formula}"` :
          ''
        return typePrettyPrinted.concat(formulaText)
      }
    }
  }
</script>

<style scoped>

</style>
