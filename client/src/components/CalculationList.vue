<!--This component is here to show the list of pending/finished calculations in a table.-->
<template>
  <table style="width: 100%;">
    <tr>
      <th>APT</th>
      <th>Calculation type</th>
      <th>Time started</th>
      <th>Time finished</th>
      <th>Status</th>
      <th>Action</th>
    </tr>
    <tr v-for="listing in calculationListings"
        :key="listing.canonicalApt">
      <td> {{ listing.canonicalApt.split('\n')[0] }}</td>
      <td>Graph Game BDD</td>
      <td>{{ formatDate(listing.timeStarted) }}</td>
      <td>{{ formatDate(listing.timeFinished) }}</td>
      <td>{{ listing.calculationStatus }}</td>
      <td v-if="listing.calculationStatus === 'COMPLETED'">
        <button @click="$emit('loadGraphGameBdd', listing.canonicalApt)">Load</button>
      </td>
      <td v-else-if="['RUNNING', 'QUEUED'].includes(listing.calculationStatus)">
        <button>Cancel</button>
      </td>
      <td v-else>-</td>
    </tr>
  </table>
</template>

<script>
  const moment = require('moment')
  export default {
    name: 'CalculationList',
    props: {
      calculationListings: {
        type: Array,
        required: true
      }
    },
    methods: {
      moment,
      formatDate (secondsSinceUnixEpoch) {
        return moment.unix(secondsSinceUnixEpoch).format('LTS') // You can add 'MMM Do' to get month and day
      }
    }
  }
</script>

<style scoped>

</style>
