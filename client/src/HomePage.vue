<template>
  <v-app>
    <v-container>
      <v-row
        style="justify-content: center;"
      >
        <v-col
          cols="12"
          lg="6"
          xl="5"
        >
          <v-card>
            <v-card-title
              primary-title
              style="justify-content: space-between; word-break: unset;">
              <router-link to="/modelChecking">
                AdamMC: A Flow-LTL Model Checker for the Analysis of Distributed Asynchronous
                Models
              </router-link>
            </v-card-title>
            <v-card-text>
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
            </v-card-text>
          </v-card>

        </v-col>
        <v-col
          cols="12"
          lg="6"
          xl="5"
        >
          <v-card>
            <v-card-title
              primary-title
              style="justify-content: space-between; word-break: unset;">
              <router-link to="/distributedSynthesis"> AdamSYNT: Causality-Based Synthesis of
                Distributed Asynchronous Systems
              </router-link>
            </v-card-title>
            <v-card-text>
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
            </v-card-text>

          </v-card>
        </v-col>
      </v-row>
    </v-container>
  </v-app>
</template>

<script>
  export default {
    name: 'home-page'
  }
</script>

<style scoped>

</style>
