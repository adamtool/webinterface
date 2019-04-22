<template>
  <div style="">
    <p>
      This version of ADAM Web was built at {{ formattedBuildDate }}
      using git revision {{ sourceCodeRevision }}.
    </p>

    <p>
      ADAM Web was developed by Ann Yanich under the supervision of Manuel Gieseking in the
      department Entwicklung korrekter Systeme [Correct System Design] at the Universität Oldenburg.
    </p>

    <p>
      ADAM Web is based on the library
      <a href="https://uol.de/csd/adam/" target="_blank">
        ADAM (Analyzer of Distributed Asynchronous Models).
      </a>
    </p>

    <p>
      ADAM Web uses open source components. You can find the source code of their open source
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
  </div>
</template>

<script>
  import creditsJson from '../assets/licenses-json.json'
  import { format } from 'date-fns'

  export default {
    name: 'AboutAdamWeb',
    computed: {
      creditsJson: () => creditsJson,
      buildDate: () => Date.parse(____ADAM_WEB_BUILD_DATE____),
      formattedBuildDate: function () {
        return format(this.buildDate, 'HH:mm:ss on MMMM Do YYYY ')
      },
      sourceCodeRevision: () => ____ADAM_WEB_BUILD_SHA____
    },
  }
</script>

<style scoped>

</style>
