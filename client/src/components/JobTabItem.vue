<template>
  <div v-if="tab.type === 'errorMessage'">
    Error: {{ tab.message }}
  </div>
  <v-card
    class="job-tab-card"
    v-else-if="tab.jobStatus !== 'COMPLETED'"
  >
    <v-card-title
      class="job-tab-card-title">
      {{ textForJobStatusInTab(tab.jobStatus) }}
    </v-card-title>

    <v-card-text
      class="job-tab-card-text"
    >
      <div v-if="tab.type === 'MODEL_CHECKING_RESULT'">
        <div>Checking the following formula:
          <strong>{{ tab.jobKey.requestParams.formula }}</strong>
        </div>
      </div>
      <div v-if="tab.jobStatus === 'QUEUED'">
        Queue position: <strong>{{ tab.queuePosition }}</strong>
      </div>
      <div v-if="tab.jobStatus === 'FAILED'">
        Failure reason: <strong>{{ tab.failureReason }}</strong>
      </div>
    </v-card-text>

    <v-card-actions>
      <v-btn
        color="blue lighten-3"
        @click="$emit('loadEditorNetFromApt', tab.jobKey.canonicalApt)"
      >
        {{ useModelChecking ? 'View Petri Net with Transits' : 'View Petri Game' }}
      </v-btn>
      <v-btn
        v-if="tab.jobStatus === 'QUEUED' || tab.jobStatus === 'RUNNING'"
        color="red lighten-2"
        @click="$emit('cancelJob', tab.jobKey)"
      >
        Cancel Job
      </v-btn>
    </v-card-actions>
  </v-card>
  <div v-else-if="tab.type === 'EXISTS_WINNING_STRATEGY'">
    <template v-if="tab.result === true">
      Yes, there is a winning strategy for this net.
    </template>
    <template v-else-if="tab.result === false">
      No, there is no winning strategy for this net.
    </template>
    <template v-else>
      Error: Invalid value for job result: {{ tab.result }}
      <div>
        (This shouldn't happen. Please file a bug. :)
      </div>
    </template>
  </div>
  <Simulator v-else-if="tab.type === 'WINNING_STRATEGY'"
             ref="winningStratSimulator"
             editorMode="Simulator"
             :useModelChecking="false"
             netType="PETRI_GAME"
             :graph="tab.result.graph"
             :jobKey="tab.jobKey"
             :restEndpoints="restEndpoints"
             :showPhysicsControls="showPhysicsControls"
             :showPartitions="showPartitions"
             :showNodeLabels="showNodeLabels"
             :isVisible="isTabSelected"
             :additionalSaveActions="saveActionsWinningStrategy(tab.jobKey)"
  />
  <GraphEditor v-else-if="tab.type === 'GRAPH_STRATEGY_BDD'"
               :graph="tab.result"
               editorMode="Viewer"
               :restEndpoints="restEndpoints"
               :showPhysicsControls="showPhysicsControls"
               :showPartitions="showPartitions"
               :showNodeLabels="showNodeLabels"
               :isVisible="isTabSelected"
  />
  <GraphEditor v-else-if="tab.type === 'GRAPH_GAME_BDD'"
               :graph='tab.result'
               editorMode="Viewer"
               @toggleStatePostset="stateId => $emit('toggleStatePostset', stateId)"
               @toggleStatePreset="stateId => $emit('toggleStatePreset', stateId)"
               :restEndpoints="restEndpoints"
               :showPhysicsControls="showPhysicsControls"
               :showPartitions="showPartitions"
               :showNodeLabels="showNodeLabels"
               :repulsionStrengthDefault="415"
               :linkStrengthDefault="0.04"
               :gravityStrengthDefault="300"
               :isVisible="isTabSelected"
  />
  <Simulator v-else-if="tab.type === 'MODEL_CHECKING_NET'"
             ref="mcNetSimulator"
             editorMode="Simulator"
             :graph="tab.result.graph"
             :jobKey="tab.jobKey"
             :useModelChecking="true"
             netType="PETRI_NET"
             :restEndpoints="restEndpoints"
             :showPhysicsControls="showPhysicsControls"
             :showPartitions="showPartitions"
             :showNodeLabels="showNodeLabels"
             :isVisible="isTabSelected"
             :additionalSaveActions="saveActionsMcNet(tab.jobKey)"
  />
  <v-card v-else-if="tab.type === 'MODEL_CHECKING_FORMULA'"
          class="job-tab-card"

  >
    {{ tab.result }}
  </v-card>
  <v-card v-else-if="tab.type === 'MODEL_CHECKING_RESULT'"
          class="job-tab-card"
  >
    <v-card-title class="job-tab-card-title">
      Model checking result
    </v-card-title>
    <v-card-text class="job-tab-card-text">
      <div>
        <!--<span>Formula: <strong>{{ tab.jobKey.requestParams.formula }}</strong></span>-->
        <span>Formula: <strong>{{ tab.result.formulaRepresentation }}</strong></span>
      </div>
      <div>
                  <span>Result:
                    <span :style="`color: ${modelCheckingResultColor(tab.result.satisfied)}`">
                      <strong>{{ formatSatisfied(tab.result.satisfied) }}</strong>
                    </span>
                  </span>
      </div>

      <v-list
        class="left-padding-0-descendants elevation-2 accordion-list"
      >


        <!--Counterexample of input net.  It should be open by default.-->
        <v-list-group
          v-if="tab.result.counterExample"
          :value="true"
        >
          <template v-slot:activator>
            <v-list-item>
              <v-list-item-content>
                <v-list-item-title>
                  Counter Example (Input Petri Net with Transits)
                </v-list-item-title>
              </v-list-item-content>
            </v-list-item>
          </template>
          <v-list-item
            class="list-tile-stretchy"
          >
            <v-list-item-content>
              <div
                class="counter-example"
              >{{ tab.result.reducedCexInputNet }}
              </div>
            </v-list-item-content>
          </v-list-item>
          <v-list-item
            style="margin-top: 0;"
          >
            <v-list-item-content
              style="padding-top: 0;"
            >
              <!-- TODO #50 Implement loading of counter example into simulator -->
              <v-btn
                color="blue lighten-3"
                @click="$emit('loadCxInSimulator', {
                  jobKey: tab.jobKey,
                  cxType: 'INPUT_NET'
                })"
              >
                Load into simulator
              </v-btn>
            </v-list-item-content>
          </v-list-item>
          <v-list-item
            style="margin-top: 0;"
          >
            <v-list-item-content
              style="padding-top: 0;"
            >
              <v-tooltip
                bottom
              >
                <template v-slot:activator="{ on }">
                  <v-btn
                    color="green"
                    @click="$emit('saveWitnessesPdf', { jobKey: tab.jobKey })"
                    v-on="on">
                    Show witnesses
                  </v-btn>
                </template>
                Download a PDF which shows the witnesses for this counter example.
              </v-tooltip>
            </v-list-item-content>
          </v-list-item>
        </v-list-group>

        <!--Counterexample for constructed net.-->
        <v-list-group
          v-if="tab.result.counterExample"
        >
          <template v-slot:activator>
            <v-list-item>
              <v-list-item-content>
                <v-list-item-title>
                  Counter Example (Constructed Petri Net)
                </v-list-item-title>
              </v-list-item-content>
            </v-list-item>
          </template>
          <v-list-item
            class="list-tile-stretchy"
          >
            <v-list-item-content>
              <div
                class="counter-example"
                v-if="expandCexMc"
                @click="expandCexMc = false"
              ><!-- @formatter:off -->{{ tab.result.counterExample }}</div>
              <!-- @formatter:on -->
              <v-tooltip
                v-else
                bottom
                style="display: none;"
              >
                <template #activator="data">
                  <div
                    v-on="data.on"
                    class="counter-example highlightable"
                    @click="expandCexMc = true"
                  ><!-- @formatter:off -->{{ tab.result.reducedCexMc }}<v-icon>more</v-icon></div>
                  <!-- @formatter:on -->
                </template>
                <div>Click to expand</div>
              </v-tooltip>
            </v-list-item-content>
          </v-list-item>
          <v-list-item
            style="margin-top: 0;"
          >
            <v-list-item-content
              style="padding-top: 0;"
            >
              <!-- TODO #50 Implement loading of counter example into simulator -->
              <v-btn
                color="blue lighten-3"
                @click="$emit('loadCxInSimulator', {
                  jobKey: tab.jobKey,
                  cxType: 'MODEL_CHECKING_NET'
                })"
              >
                Load into simulator
              </v-btn>
            </v-list-item-content>
          </v-list-item>
        </v-list-group>

        <!--Expandable statistics panel-->
        <v-list-group
          v-if="tab.result.statistics"
        >
          <template v-slot:activator>
            <v-list-item
            >
              <v-list-item-content>
                <v-list-item-title>Statistics</v-list-item-title>
              </v-list-item-content>
            </v-list-item>
          </template>

          <v-list-item
            class="list-tile-stretchy"
          >
            <v-list-item-content>
              <ul>
                <li
                  v-for="(stat, statName) in tab.result.statistics"
                  :key="statName"
                >
                  {{ statName }}: <strong>{{ stat }}</strong>
                </li>
              </ul>
            </v-list-item-content>
          </v-list-item>
        </v-list-group>
      </v-list>
    </v-card-text>
    <v-card-actions>
      <v-btn
        color="blue lighten-3"
        @click="$emit('loadEditorNetFromApt', tab.jobKey.canonicalApt)"
      >
        {{ useModelChecking ? 'View Petri Net with Transits' : 'View Petri Game' }}
      </v-btn>
    </v-card-actions>
  </v-card>
  <div v-else>
    <div>Tab type not yet implemented: {{ tab.type }}</div>
    <div>
      Tab contents:
      <div style="white-space: pre-wrap;">{{ tab }}</div>
    </div>
  </div>
</template>

<script>
  import GraphEditor from './GraphEditor'
  import Simulator from './Simulator'
  import {modelCheckingResultColor, formatSatisfied} from '../jobType'
  import {saveFileAs} from '../fileutilities'
  import logging from '../logging'

  export default {
    name: 'JobTabItem',
    components: {
      GraphEditor,
      Simulator
    },
    data: function () {
      return {
        expandCexMc: false
      }
    },
    props: {
      tab: {
        type: Object,
        required: true
      },
      isTabSelected: {
        type: Boolean,
        required: true
      },
      showPhysicsControls: {
        type: Boolean,
        required: true
      },
      showPartitions: {
        type: Boolean,
        required: true
      },
      showNodeLabels: {
        type: Boolean,
        required: true
      },
      restEndpoints: {
        type: Object,
        required: true
      },
      useModelChecking: {
        type: Boolean,
        required: true
      }
    },
    methods: {
      modelCheckingResultColor,
      formatSatisfied,
      textForJobStatusInTab: function (jobStatus) {
        switch (jobStatus) {
          case 'NOT_STARTED':
            return 'This job has not yet been queued to be run.'
          case 'FAILED':
            return 'This job failed.'
          case 'RUNNING':
            return 'This job is running.'
          case 'QUEUED':
            return 'This job is currently waiting to be run.'
          case 'CANCELING':
            return 'This job is being canceled.'
          case 'CANCELED':
            return 'This job has been canceled.'
          case 'COMPLETED':
            return 'This job is finished.'
        }
      },
      saveJobAsApt: function (jobKey, getNodePositions, filename) {
        const nodePositions = getNodePositions()
        const requestPromise = this.restEndpoints.saveJobAsApt({
          jobKey,
          nodeXYCoordinateAnnotations: nodePositions
        })
        requestPromise.then(response => {
          if (response.data.status === 'success') {
            const apt = response.data.result
            saveFileAs(apt, filename)
          } else if (response.data.status === 'error') {
            logging.sendErrorNotification(response.data.message)
          } else {
            logging.sendErrorNotification('Invalid response from server')
          }
        }).catch(logging.sendErrorNotification)
        return requestPromise
      },
      saveActionsMcNet: function (jobKey) {
        return [{
          label: 'Save as APT',
          callback: () => this.saveJobAsApt(
            jobKey,
            this.$refs.mcNetSimulator.$refs.graphEditor.getNodeXYCoordinatesFixed,
            'model-checking-net.apt')
        }]
      },
      saveActionsWinningStrategy: function (jobKey) {
        return [{
          label: 'Save as APT',
          callback: () => this.saveJobAsApt(
            jobKey,
            this.$refs.winningStratSimulator.$refs.graphEditor.getNodeXYCoordinatesFixed,
            'winning-strategy.apt')
        }]
      }
    }
  }
</script>

<style scoped>

  .job-tab-card {
    display: flex;
    flex-direction: column;
    height: 100%;
  }

  .job-tab-card-text {
    font-size: 16px;
    padding-top: 8px;
    flex-grow: 1;
    flex-shrink: 1;
    flex-basis: 1px;
    overflow-y: auto;
  }

  .job-tab-card-title {
    padding-bottom: 8px;
  }

  .job-tab-card-text div + div {
    margin-top: 5px;
  }

  .counter-example {
    font-size: 16px;
    line-height: 1.4rem;
    white-space: pre-wrap;
  }

  .counter-example::first-line {
    font-weight: bold;
  }

  .list-tile-stretchy > * {
    height: auto;
  }

  .list-tile-smaller > * {
    min-height: 32px;
  }

  .left-padding-0-descendants * {
    padding-left: 0;
  }

  .accordion-list {
    padding-left: 15px;
  }

</style>
