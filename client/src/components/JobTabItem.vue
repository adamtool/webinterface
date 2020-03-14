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
        @click="$emit('loadPetriGameFromApt', tab.jobKey.canonicalApt)"
      >
        View Petri Game
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
  <GraphEditor v-else-if="tab.type === 'WINNING_STRATEGY'"
               netType="PETRI_GAME"
               :graph="tab.result"
               editorMode="Viewer"
               :restEndpoints="restEndpoints"
               :shouldShowPhysicsControls="showPhysicsControls"/>
  <GraphEditor v-else-if="tab.type === 'GRAPH_STRATEGY_BDD'"
               :graph="tab.result"
               editorMode="Viewer"
               :restEndpoints="restEndpoints"
               :shouldShowPhysicsControls="showPhysicsControls"/>
  <!--TODO replace anonymous functions with non-anonymous ones for performance reasons-->
  <GraphEditor v-else-if="tab.type === 'GRAPH_GAME_BDD'"
               :graph='tab.result'
               editorMode="Viewer"
               @toggleStatePostset="stateId => $emit('toggleStatePostset', stateId)"
               @toggleStatePreset="stateId => $emit('toggleStatePreset', stateId)"
               :restEndpoints="restEndpoints"
               :shouldShowPhysicsControls="showPhysicsControls"
               :repulsionStrengthDefault="415"
               :linkStrengthDefault="0.04"
               :gravityStrengthDefault="300"/>
  <GraphEditor v-else-if="tab.type === 'MODEL_CHECKING_NET'"
               :graph="tab.result.graph"
               :petriNetApt="tab.result.apt"
               netType="PETRI_NET"
               editorMode="Simulator"
               :restEndpoints="restEndpoints"
               :shouldShowPhysicsControls="showPhysicsControls"/>
  <v-card v-else-if="tab.type === 'MODEL_CHECKING_RESULT'"
          class="job-tab-card"
  >
    <v-card-title class="job-tab-card-title">
      Model checking result
    </v-card-title>
    <v-card-text class="job-tab-card-text">
      <div>
        <span>Formula: <strong>{{ tab.jobKey.requestParams.formula }}</strong></span>
      </div>
      <div>
                  <span>Result:
                    <span :style="`color: ${modelCheckingResultColor(tab.result.satisfied)}`">
                      <strong>{{ tab.result.satisfied }}</strong>
                    </span>
                  </span>
      </div>

      <v-list
        class="left-padding-0-descendants elevation-2 accordion-list"
      >
        <!--Expandable counterexample.  It should be open by default-->
        <v-list-group
          v-if="tab.result.counterExample"
          :value="true"
        >
          <template v-slot:activator>
            <v-list-item
            >
              <v-list-item-content>
                <v-list-item-title>Counter example</v-list-item-title>
              </v-list-item-content>
            </v-list-item>
          </template>
          <v-list-item
            class="list-tile-stretchy"
          >
            <v-list-item-content>
              <div class="counter-example">{{ tab.result.counterExample }}</div>
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
        @click="$emit('loadPetriGameFromApt', tab.jobKey.canonicalApt)"
      >
        View Petri Game
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
  import {modelCheckingResultColor} from '../jobType'
  export default {
    name: 'JobTabItem',
    components: {
      GraphEditor
    },
    data: function () {
      return {}
    },
    props: {
      tab: {
        type: Object,
        required: true
      },
      showPhysicsControls: {
        type: Boolean,
        required: true
      },
      restEndpoints: {
        type: Object,
        required: true
      }
    },
    methods: {
      modelCheckingResultColor,
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
