<template>
  <div style="">
    <p>
      AdamWEB is a frontend for the command-line tool
      <a href="https://github.com/adamtool/adam" target="_blank">
        Adam (Analyzer of Distributed Asynchronous Models)
      </a>.
    <span v-if="useModelChecking">
        The model checking approach is based on the algorithms of the command-line tool
      <a href="https://github.com/adamtool/adammc" target="_blank">
        AdamMC
      </a>.
    </span>
    </p>
      <v-expansion-panels >
        <v-expansion-panel v-if="useModelChecking" value="true">
          <v-expansion-panel-header>
            Overview
          </v-expansion-panel-header>
          <v-expansion-panel-content>
                <p><strong>AdamMC</strong> is a model checker for asynchronous distributed systems
                  modeled with Petri nets with transits and specifications given in Flow-LTL.</p>
                <p><strong>Petri nets with transits</strong> extend standard Petri nets with a
                  transit relation to model the local data flow in asynchronous distributed
                  systems in addition to the global control flow.</p>
                <p><strong>Flow-LTL</strong> is a specification language for Petri nets with
                  transits and allows specifying linear properties on both the global and
                  the local view of the system. In particular, it is possible to use LTL to globally
                  select desired runs of the system (e.g., only fair and maximal runs) and check the
                  local data flow of only those selected runs against LTL.</p>
                <p>Internally, the problem is reduced to a verification problem of circuits which
                  is checked by <a href="https://people.eecs.berkeley.edu/~alanmi/abc/">ABC</a> using its large toolbox of
                  hardware
                  verification algorithms. <strong>Software defined networks</strong> are, due to
                  the separation of data and control plane, a natural application domain for Petri
                  nets with transits and Flow-LTL.</p>
                <p>
                <h2>Features:</h2>
            <p style="margin-bottom: 2mm;"></p>
                <ul>
                  <li>Modeling, visualization, and simulation of Petri nets with transits</li>
                  <li>Model checking of Petri nets with transits against Flow-LTL</li>
                  <li>Model checking of 1-bounded Petri nets against LTL with places and transitions
                    as atomic propositions
                  </li>
                  <li>Visualization and simulation of counter examples</li>
                </ul>
                </p>
                <p>
                <h2>Documentation:</h2>
            <p style="margin-bottom: 2mm;"></p>
                <ul>
                  <li><a href="https://github.com/adamtool/webinterface/tree/master/doc/mc">https://github.com/adamtool/webinterface/tree/master/doc/mc</a>
                  </li>
                </ul>
                </p>
                <p>
                <h2>Related Publications: </h2>
            <p style="margin-bottom: 2mm;"></p>
                <ul>
                  <li><em>Bernd Finkbeiner, Manuel Gieseking, Jesko Hecking-Harbusch,
                    Ernst-Rüdiger
                    Olderog:</em>
                    <a href="https://doi.org/10.1007/978-3-030-53291-8_5">AdamMC: A
                      Model
                      Checker for Petri Nets with Transits against Flow-LTL</a>. CAV (2) 2020:
                    64-76 (<a
                      href="https://arxiv.org/abs/2005.07130">full version</a>)
                  </li>
                  <li><em>Bernd Finkbeiner, Manuel Gieseking, Jesko Hecking-Harbusch,
                    Ernst-Rüdiger
                    Olderog:</em>
                    <a href="https://doi.org/10.1007/978-3-030-31784-3_30"> Model Checking
                      Data Flows in Concurrent Network Updates</a>. ATVA 2019: 515-533 (<a
                      href="http://arxiv.org/abs/1907.11061">full version</a>)
                  </li>
                </ul>
                </p>

              <p>
                      <h2>Authors: </h2>
            <p style="margin-bottom: 2mm;"></p>
            <ul>
              <li>
                The web interface AdamWEB is developed by
                <a href="mailto:ann.yanich@posteo.de" target="_blank">
                  Ann Yanich
                </a>
                under the supervision of
                <a href="https://uol.de/en/csd/persons-contacts/manuel-gieseking-msce" target="_blank">
                  Manuel Gieseking
                </a>
                in the <a href="https://uol.de/en/computingscience/csd" target="_blank">Correct System Design Group</a> at the University of Oldenburg.
              </p>
              </li>
              <li>
                The back end is a cooperation of the <a href="https://www.react.uni-saarland.de/" target="_blank">Reactive Systems Group</a> of Saarland University and the
                <a href="https://uol.de/en/computingscience/csd" target="_blank">Correct System Design Group</a> of the University of Oldenburg by
                <a href="https://www.react.uni-saarland.de/people/finkbeiner.html" target="_blank">Bernd Finkbeiner</a>,
                <a href="https://uol.de/en/csd/persons-contacts/manuel-gieseking-msc" target="_blank">Manuel Gieseking</a>,
                <a href="https://www.react.uni-saarland.de/people/hecking-harbusch.html" target="_blank">Jesko Hecking-Harbusch</a>, and
                <a href="https://uol.de/en/computingscience/csd/persons-contacts/prof-dr-ernst-ruediger-olderog" target="_blank">Ernst-Rüdiger Olderog</a>.
              </li>
            </ul>
       </v-expansion-panel-content>
      </v-expansion-panel>
      <v-expansion-panel>
        <v-expansion-panel-header>
          Version
        </v-expansion-panel-header>
        <v-expansion-panel-content>
          <p>
            Build date: {{ formattedBuildDate }}
          </p>
          <p>
            Git revision: {{ sourceCodeRevision }}.
          </p>
        </v-expansion-panel-content>
      </v-expansion-panel>
      <v-expansion-panel>
        <v-expansion-panel-header>
          License information
        </v-expansion-panel-header>
        <v-expansion-panel-content>
          <p>
            AdamWEB uses open source components. You can find the source code of their open
            source
            projects along with license information below.
          </p>
          <ul>
            <li v-for="(library, libraryName) in creditsJson">
              {{ libraryName }}
              <ul>
                <li>License: <a :href="library.licenseUrl">{{ library.licenses }}</a></li>
                <li><a :href="library.repository">Repository</a></li>
              </ul>
            </li>
          </ul>
        </v-expansion-panel-content>
      </v-expansion-panel>
    </v-expansion-panels>
  </div>
</template>

<script>
  import creditsJson from '../assets/licenses-json.json'
  import {format} from 'date-fns'

  export default {
    name: 'AboutAdamWeb',
    computed: {
      creditsJson: () => creditsJson,
      buildDate: () => Date.parse(____ADAM_WEB_BUILD_DATE____),
      formattedBuildDate: function () {
        return format(this.buildDate, 'HH:mm:ss, MMMM Do, YYYY ')
      },
      sourceCodeRevision: () => ____ADAM_WEB_BUILD_SHA____
    },
    props: {
      useModelChecking: {
        type: Boolean,
        required: true
      }
    }
  }
</script>

<style scoped>

</style>
