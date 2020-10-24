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
      <span v-if="!useModelChecking">
        The synthesis of distributed systems approach is based on the algorithms of the command-line tool
      <a href="https://github.com/adamtool/adamsynt" target="_blank">
        AdamSYNT
      </a>.
    </span>
    </p>
      <v-expansion-panels multiple v-model="openPanels">
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
                  Ann Yanich</a>
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

        <v-expansion-panel v-if="!useModelChecking" value="true">
          <v-expansion-panel-header>
            Overview
          </v-expansion-panel-header>
          <v-expansion-panel-content>
            <p><strong>AdamSYNT</strong> is a synthesizer for asynchronous distributed systems
              modeled with
              Petri games.</p>
            <p><strong>Petri games</strong> extend Petri nets with a game semantics. The
              tokens of the net are the players in the game. Players remember their own causal
              past and do not know anything about the others as long as they do not communicate
              (whereby a communication is modelled as the participation in a joint transition).
              During a communication, all information is passed to all participating players.
            </p>
            <p>For <strong>local safety winning conditions</strong>, the synthesis problem for
              Petri games with a bounded number of controllable components and one
              uncontrollable component is solved, i.e., a local controller for each component
              is created iff a solution exists.</p>
            <p>Internally, the problem is reduced to a two-player game over a finite graph
              with complete information, and <strong>BDD</strong> solving algorithms are used
              to obtain a strategy.</p>
            <h2>Features:</h2>
            <p style="margin-bottom: 2mm;"></p>
            <ul>
              <li>Modeling, visualization, and simulation of Petri games</li>
              <li>Synthesis of Petri games with one environment player and a bounded number of
                system players with a local safety objective
              </li>
              <li>Interactive visualization of the corresponding two-player game to aid in
                finding modeling bugs
              </li>
              <li>Visualization and simulation of the strategies</li>
            </ul>
            <p>
            <h2>Documentation:</h2>
            <p style="margin-bottom: 2mm;"></p>
            <ul>
              <li><a href="https://github.com/adamtool/webinterface/tree/master/doc/synt">https://github.com/adamtool/webinterface/tree/master/doc/synt</a>
              </li>
            </ul>
            </p>
            <p>
            <h2>Related Publications:</h2>
            <p style="margin-bottom: 2mm;"></p>
            <ul>
              <li><em>Bernd Finkbeiner, Manuel Gieseking, Ernst-Rüdiger Olderog:</em><br>
                <a href="https://doi.org/10.1007/978-3-319-21690-4_25">Adam:
                  Causality-Based Synthesis of Distributed Systems</a>. CAV (1) 2015: 433-439
              </li>
              <li><em>Bernd Finkbeiner, Manuel Gieseking, Jesko Hecking-Harbusch,
                Ernst-Rüdiger Olderog:</em><br>
                <a href="https://doi.org/10.4204/EPTCS.260.5">Symbolic vs.
                  Bounded Synthesis for Petri Games</a>. SYNT@CAV 2017: 23-43
              </li>
              <li><em>Bernd Finkbeiner, Ernst-Rüdiger Olderog:</em><br>
                <a href="https://doi.org/10.1016/j.ic.2016.07.006">Petri games:
                  Synthesis of distributed systems with causal memory</a>. Inf. Comput. 253:
                181-203 (2017)
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
                  Ann Yanich</a>
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
    data: function () {
      return {
        openPanels: [0]
      }
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
