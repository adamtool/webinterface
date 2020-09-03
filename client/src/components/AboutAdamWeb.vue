<template>
  <div style="">
    <p>
      AdamWEB is a frontend for the library
      <a href="https://uol.de/csd/adam/" target="_blank">
        ADAM (Analyzer of Distributed Asynchronous Models).
      </a>
    </p>

    <p>
      It was developed by
      <a href="mailto:ann.yanich@posteo.de" target="_blank">
        Ann Yanich
      </a>
      under the supervision of
      <a href="mailto:manuel.gieseking@informatik.uni-oldenburg.de" target="_blank">
        Manuel Gieseking
      </a>
      in the Correct System Design group at the Universität Oldenburg.
    </p>

    <v-expansion-panels>
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
    }
  }
</script>

<style scoped>

</style>
