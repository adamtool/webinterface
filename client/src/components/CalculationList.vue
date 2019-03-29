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
      <td>{{ formatCalculationType(listing.type) }}</td>
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
              <v-icon>more</v-icon>
            </td>
          </template>
          <div>{{ listing.failureReason }}</div>
        </v-tooltip>
      </template>
      <template
        v-else-if="listing.calculationStatus === 'COMPLETED'
                        && listing.type === 'EXISTS_WINNING_STRATEGY'">
        <td :style="`color: ${listing.result ? 'blue' : 'red'}`">
          {{ listing.result ? '(There is a strategy)' : '(There is no strategy)'}}
        </td>
      </template>
      <template v-else>
        <td>{{ listing.calculationStatus }}</td>
      </template>

      <!--Button to load the result of a calculation or cancel a pending calculation-->
      <td v-if="listing.calculationStatus === 'COMPLETED' && listing.type === 'GRAPH_GAME_BDD'"
          class="highlightable">
        <button @click="$emit('getGraphGameBdd', listing.canonicalApt)">Load</button>
      </td>
      <td
        v-else-if="listing.calculationStatus === 'COMPLETED'
                     && listing.type === 'WINNING_STRATEGY'"
        class="highlightable">
        <button @click="$emit('getWinningStrategy', listing.canonicalApt)">Load</button>
      </td>
      <td
        v-else-if="listing.calculationStatus === 'COMPLETED'
                     && listing.type === 'GRAPH_STRATEGY_BDD'"
        class="highlightable">
        <button @click="$emit('getGraphStrategyBdd', listing.canonicalApt)">Load</button>
      </td>
      <td v-else-if="['RUNNING', 'QUEUED'].includes(listing.calculationStatus)"
          class="highlightable">
        <button @click="$emit('cancelCalculation',
        { canonicalApt: listing.canonicalApt, type: listing.type})">
          Cancel
        </button>
      </td>
      <td v-else>-</td>
    </tr>
  </table>
</template>

<script>
  import {format} from 'date-fns'

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
      },
      formatCalculationType (calculationType) {
        switch (calculationType) {
          case 'GRAPH_GAME_BDD':
            return 'Graph Game BDD'
          case 'EXISTS_WINNING_STRATEGY':
            return 'Exists winning strategy?'
          case 'WINNING_STRATEGY':
            return 'Winning Strategy'
          case 'GRAPH_STRATEGY_BDD':
            return 'Graph Strategy BDD'
        }
        return calculationType
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
