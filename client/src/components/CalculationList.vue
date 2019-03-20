<!--This component is here to show the list of pending/finished calculations in a table.-->
<template>
  <table class="calculation-list-table" style="width: 100%;">
    <tr>
      <th>APT</th>
      <th>Calculation type</th>
      <th>Time started</th>
      <th>Time finished</th>
      <th>Status</th>
      <th>Action</th>
    </tr>
    <tr v-for="listing in calculationListings"
        :key="`${listing.canonicalApt}%${listing.type}`">
      <td>{{ listing.canonicalApt.split('\n')[0] }}</td>
      <td>{{ listing.type }}</td>
      <td>{{ formatDate(listing.timeStarted) }}</td>
      <td>{{ formatDate(listing.timeFinished) }}</td>

      <!--Status column-->
      <template v-if="listing.calculationStatus === 'FAILED'">
        <!--Not sure why, but v-tooltip seems to insert an empty span element that messes up the layout
        of the table, and by putting display: none; here we can fix that. -->
        <v-tooltip bottom style="display: none;">
          <template #activator="data">
            <td v-on="data.on"
                class="highlightable">
              {{ listing.calculationStatus }}
            </td>
          </template>
          <div>{{ listing.failureReason }}</div>
        </v-tooltip>
      </template>
      <template
        v-else-if="listing.calculationStatus === 'COMPLETED' && listing.type === 'existsWinningStrategy'">
        <td :style="`color: ${listing.result ? 'blue' : 'red'}`">
          {{ listing.result ? '(There is a strategy)' : '(There is no strategy)'}}
        </td>
      </template>
      <template v-else>
        <td>{{ listing.calculationStatus }}</td>
      </template>

      <!--Button to load the result of a calculation or cancel a pending calculation-->
      <td v-if="listing.calculationStatus === 'COMPLETED' && listing.type === 'Graph Game BDD'"
          class="highlightable">
        <button @click="$emit('loadGraphGameBdd', listing.canonicalApt)">Load</button>
      </td>
      <td
        v-else-if="listing.calculationStatus === 'COMPLETED' && listing.type === 'Winning Strategy'"
        class="highlightable">
        <button @click="$emit('loadWinningStrategy', listing.canonicalApt)">Load</button>
      </td>
      <td v-else-if="['RUNNING', 'QUEUED'].includes(listing.calculationStatus)"
          class="highlightable">
        <button>Cancel</button>
      </td>
      <td v-else>-</td>
    </tr>
  </table>
</template>

<script>
  import { format } from 'date-fns'

  export default {
    name: 'CalculationList',
    props: {
      calculationListings: {
        type: Array,
        required: true
      }
    },
    methods: {
      formatDate (secondsSinceUnixEpoch) {
        if (secondsSinceUnixEpoch === 0) {
          return '-'
        }
        return format(secondsSinceUnixEpoch * 1000, 'HH:mm:ss')
        // You can add 'MMM Do' to get month and day
      }
    }
  }
</script>

<style scoped>
  .calculation-list-table {
    text-align: left;
  }

  .highlightable:hover {
    background-color: #6db8c1;
  }
  .highlightable {
    background-color: #d9fbff;
  }

</style>
