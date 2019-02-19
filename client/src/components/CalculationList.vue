<!--This component is here to show the list of pending/finished calculations in a table.-->
<template>
  <table style="width: 100%;">
    <tr>
      <th>APT</th>
      <th>Calculation type</th>
      <th>Status</th>
      <th></th>
    </tr>
    <tr v-for="listing in calculationListings"
        :key="listing.canonicalApt">
      <td @click="$emit('loadGraphGameBdd', listing.canonicalApt)">{{ listing.canonicalApt.split('\n')[0] }}</td>
      <td>Graph Game BDD</td>
      <td>{{ listing.calculationStatus }}</td>
      <td>{{ listing.calculationStatus === 'FINISHED' ? 'Load' : ''}}</td>
    </tr>
  </table>
</template>

<script>
  export default {
    name: 'CalculationList',
    props: {
      calculationListings: {
        type: Array,
        required: true
      }
    }
  }
</script>

<style scoped>

</style>
