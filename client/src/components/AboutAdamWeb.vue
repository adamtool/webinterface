<template>
  <div style="">
    <p>
      ADAM Web was developed by Ann Yanich under the supervision of Manuel Gieseking in the
      department Entwicklung korrekter Systeme [Correct System Design] at the Universität Oldenburg.
    </p>

    <p>
      ADAM Web is based on the library
      <a href="https://uol.de/csd/adam/" target="_blank">
        ADAM (Analyzer of Distributed Asynchronous Models)
      </a>
      by Bernd Finkbeiner, Manuel Gieseking and Ernst-Rüdinger Olderog.
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

  export default {
    name: 'AboutAdamWeb',
    computed: {
      creditsJson: () => creditsJson
    }
  }
</script>

<style scoped>

</style>
