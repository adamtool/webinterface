<!--This component is here to show the list of pending/finished jobs in a table.-->
<template>
  <div v-if="visibleJobListings.length === 0"
       style="text-align: center;">
    (No jobs found)
  </div>
  <table v-else
         class="job-list-table"
         style="width: 100%;">
    <tr>
      <th>APT</th>
      <th>Job type</th>
      <th>Options</th>
      <th>Time started</th>
      <th>Time finished</th>
      <th>Status</th>
      <th>Action</th>
      <th>Queue position</th>
      <th>Delete</th>
    </tr>
    <tr v-for="listing in visibleJobListings"
        :key="JSON.stringify(listing.jobKey)">
      <td>{{ listing.jobKey.canonicalApt.split('\n')[0] }}</td>
      <td>{{ formatJobType(listing.type) }}</td>

      <!--Options-->
      <template v-if="JSON.stringify(listing.jobKey.requestParams).length > 40">
        <v-tooltip bottom style="display: none;">
          <template #activator="data">
            <td v-on="data.on"
                class="highlightable">
              {{ JSON.stringify(listing.jobKey.requestParams).slice(0, 40) }}...
              <v-icon>more</v-icon>
            </td>
          </template>
          <div>{{ JSON.stringify(listing.jobKey.requestParams, null, 2) }}</div>
        </v-tooltip>
      </template>
      <template v-else>
        <td>
          {{ JSON.stringify(listing.jobKey.requestParams) }}
        </td>
      </template>

      <td>{{ formatDate(listing.timeStarted) }}</td>
      <td>{{ formatDate(listing.timeFinished) }}</td>

      <!--Status column-->
      <template v-if="listing.jobStatus === 'FAILED'">
        <!--Not sure why, but v-tooltip seems to insert an empty span element that messes up the layout
        of the table, and by putting display: none; here we can fix that. -->
        <v-tooltip bottom style="display: none;">
          <template #activator="data">
            <td v-on="data.on"
                class="highlightable">
              {{ listing.jobStatus }}
              <v-icon>more</v-icon>
            </td>
          </template>
          <div>{{ listing.failureReason }}</div>
        </v-tooltip>
      </template>
      <template
        v-else-if="listing.jobStatus === 'COMPLETED'
                        && listing.type === 'EXISTS_WINNING_STRATEGY'">
        <td :style="`color: ${listing.result ? 'blue' : 'red'}`">
          {{ listing.result ? '(There is a strategy)' : '(There is no strategy)'}}
        </td>
      </template>
      <template
        v-else-if="listing.jobStatus === 'COMPLETED'
                        && listing.type === 'MODEL_CHECKING_RESULT'">
        <td :style="`color: ${modelCheckingResultColor(listing.result.satisfied)}`">
          {{ modelCheckingResultText(listing.result) }}
        </td>
      </template>
      <template v-else>
        <td>{{ listing.jobStatus }}</td>
      </template>

      <!--Button to load the result of a job or cancel a pending job-->
      <td v-if="listing.jobStatus === 'COMPLETED' && canBeDisplayedInTab(listing.type)"
          class="highlightable">
        <button @click="$emit('loadJob', listing.jobKey)">Load</button>
      </td>
      <td v-else-if="['RUNNING', 'QUEUED'].includes(listing.jobStatus)"
          class="highlightable">
        <button @click="$emit('cancelJob', listing.jobKey)">
          Cancel
        </button>
      </td>
      <td v-else>-</td>

      <td v-if="['RUNNING', 'CANCELING'].includes(listing.jobStatus)">In progress</td>
      <td v-else-if="listing.queuePosition !== -1">{{ listing.queuePosition }}</td>
      <td v-else>-</td>

      <td>
        <button @click="$emit('deleteJob',
          { jobKey: listing.jobKey, type: listing.type})">
          Delete
        </button>
      </td>
    </tr>
  </table>
</template>

<script>
  import { format } from 'date-fns'
  import logging from '../logging'
  import { formatJobType, modelCheckingResultColor } from '../jobType'

  export default {
    name: 'JobList',
    props: {
      jobListings: {
        type: Array,
        required: true
      },
      useModelChecking: {
        type: Boolean,
        required: true
      }
    },
    computed: {
      visibleJobListings: function () {
        const visibleJobTypes = this.useModelChecking ?
          ['MODEL_CHECKING_RESULT', 'MODEL_CHECKING_NET'] :
          ['GRAPH_GAME_BDD', 'EXISTS_WINNING_STRATEGY', 'WINNING_STRATEGY', 'GRAPH_STRATEGY_BDD']
        return this.jobListings.filter(listing => {
          return visibleJobTypes.includes(listing.type)
        }).sort((a, b) => {
          // Put queued jobs first, then the running job, then jobs that are done,
          // with the oldest jobs at the bottom and newly queued jobs at the top.
          if (a.queuePosition !== -1 && b.queuePosition !== -1) {
            return b.queuePosition - a.queuePosition
          } else if (a.queuePosition !== -1) {
            return -1
          } else if (b.queuePosition !== -1) {
            return 1
          }
          return b.timeStarted - a.timeStarted
        })
      }
    },
    methods: {
      canBeDisplayedInTab (jobType) {
        const displayableJobTypes =
          ['GRAPH_GAME_BDD', 'WINNING_STRATEGY', 'GRAPH_STRATEGY_BDD',
          'MODEL_CHECKING_NET', 'MODEL_CHECKING_RESULT']
        return displayableJobTypes.includes(jobType)
      },
      formatDate (secondsSinceUnixEpoch) {
        if (secondsSinceUnixEpoch === 0) {
          return '-'
        }
        return format(secondsSinceUnixEpoch * 1000, 'HH:mm:ss')
        // You can add 'MMM Do' to get month and day
      },
      formatJobType,
      modelCheckingResultColor,
      modelCheckingResultText (result) {
        return result.satisfied
      }
    }
  }
</script>

<style scoped>
  .job-list-table {
    text-align: left;
  }

  .highlightable:hover {
    background-color: #6db8c1;
  }

  .highlightable {
    background-color: #d9fbff;
  }

</style>
