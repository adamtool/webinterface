<template>
  <div style="">
    <v-expansion-panels multiple
                        v-model="openPanels"
    >
      <v-expansion-panel value="true">
        <v-expansion-panel-header>
          Documentation
        </v-expansion-panel-header>
        <v-expansion-panel-content>
          <a target="_blank"
             :href="useModelChecking ? 'https://github.com/adamtool/webinterface/tree/master/doc/mc' :
                                       'https://github.com/adamtool/webinterface/tree/master/doc/synt'">
            User guide (opens in new tab)
          </a>
        </v-expansion-panel-content>
      </v-expansion-panel>
      <v-expansion-panel value="true">
        <v-expansion-panel-header>
          Keyboard shortcuts
        </v-expansion-panel-header>
        <v-expansion-panel-content>
          <ul>
            <li>
              <strong>Shift + click and drag:</strong> Pan the view of the whole graph.
            </li>
            <li>
              <strong>Scroll wheel:</strong> Zoom in and zoom out.
            </li>
            <li>
              <strong>Delete:</strong> Delete the selected nodes.
            </li>
            <li>
              <strong>Hold Ctrl while clicking and dragging a node:</strong> Snap node to grid.
            </li>
          </ul>
        </v-expansion-panel-content>
      </v-expansion-panel>
      <v-expansion-panel v-if="useModelChecking">
        <v-expansion-panel-header>
          Flow-LTL syntax
        </v-expansion-panel-header>
        <v-expansion-panel-content>
          <p>
            The syntax for Flow-LTL is given by:
          </p>
          <p class="code">phi = LTL | ( phi AND phi ) | ( phi OR phi ) | ( LTL -> phi ) | A LTL</p>
          <p>with standard LTL formulas with the following operators:</p>
          <table role="table" class="ltl-formula-table">
            <thead>
            <tr>
              <th>LTL</th>
              <th>Syntax</th>
            </tr>
            </thead>
            <tbody>
            <tr>
              <td>true</td>
              <td>TRUE</td>
            </tr>
            <tr>
              <td>false</td>
              <td>FALSE</td>
            </tr>
            <tr>
              <td>atoms</td>
              <td>place and transition ids</td>
            </tr>
            <tr>
              <td>Negation</td>
              <td>NEG or !</td>
            </tr>
            <tr>
              <td>Next</td>
              <td>X</td>
            </tr>
            <tr>
              <td>Conjunction</td>
              <td>AND</td>
            </tr>
            <tr>
              <td>Disjunction</td>
              <td>OR</td>
            </tr>
            <tr>
              <td>Implication</td>
              <td>IMP or -&gt;</td>
            </tr>
            <tr>
              <td>Bimplication</td>
              <td>BIMP or &lt;-&gt;</td>
            </tr>
            <tr>
              <td>Until</td>
              <td>U</td>
            </tr>
            <tr>
              <td>Weak Until</td>
              <td>W</td>
            </tr>
            <tr>
              <td>Release</td>
              <td>R</td>
            </tr>
            </tbody>
          </table>
          <p>Binary operators have to be in brackets, unary operators not. For example (!phi0 -&gt; (phi1 AND G F
            phi2)). Note that the ids for the places and transitions are more restricted for the formula as for the web
            interface.</p>
        </v-expansion-panel-content>
      </v-expansion-panel>
    </v-expansion-panels>
  </div>
</template>

<script>
  export default {
    name: 'Help',
    data: function () {
      return {
        openPanels: [0, 1]
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
  .code {
    /*white-space: pre-wrap;*/
    font-family: Inconsolata, monospace;
    text-align: center;
  }

  .ltl-formula-table {
    margin-left: auto;
    margin-right: auto;
    margin-bottom: 10px;
  }

  .ltl-formula-table tr {
    border-top: 1px solid #bbb;
  }

  .ltl-formula-table td, .ltl-formula-table th {
    border: 1px solid #bbb;
    padding: 6px 13px;
  }

  .ltl-formula-table tr:nth-child(even) {
    background: #CCC;
  }

  .ltl-formula-table tr:nth-child(odd) {
    background: #FFF;
  }

</style>
