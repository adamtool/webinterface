<template>
  <div style="">
    <p>
      ADAM Web is a frontend for the library
      <a href="https://uol.de/csd/adam/" target="_blank">
        ADAM (Analyzer of Distributed Asynchronous Models).
      </a>
    </p>

    <p>
      It was developed by Ann Yanich under the supervision of Manuel Gieseking in the
      Correct System Design group at the Universität Oldenburg.
    </p>

    <v-expansion-panel>
      <v-expansion-panel-content>
        <template v-slot:header>
          <div>Version</div>
        </template>
        <v-card>
          <v-card-text>
            <p>
              Build date: {{ formattedBuildDate }}
            </p>
            <p>
              Git revision: {{ sourceCodeRevision }}.
            </p>
          </v-card-text>
        </v-card>
      </v-expansion-panel-content>
      <v-expansion-panel-content>
        <template v-slot:header>
          <div>License information</div>
        </template>
        <v-card>
          <v-card-text>
            <p>
              ADAM Web uses open source components. You can find the source code of their open
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
          </v-card-text>
        </v-card>
      </v-expansion-panel-content>
    </v-expansion-panel>
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
