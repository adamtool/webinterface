<template>
  <div style="">
    <p>
      This is a
      <a href="https://uol.de/csd/adam/" target="_blank">
        placeholder link.
      </a>
    </p>

    <p>
      Placeholder p
    </p>

    <v-expansion-panels>
      <v-expansion-panel>
        <v-expansion-panel-header>
          Placeholder 2
        </v-expansion-panel-header>
        <v-expansion-panel-content>
          <p>
            Placeholder text
          </p>
          <p>
            Placeholder text
          </p>
        </v-expansion-panel-content>
      </v-expansion-panel>
      <v-expansion-panel>
        <v-expansion-panel-header>
          Placeholder 1
        </v-expansion-panel-header>
        <v-expansion-panel-content>
          <p>
            Placeholder text
          </p>
        </v-expansion-panel-content>
      </v-expansion-panel>
    </v-expansion-panels>
  </div>
</template>

<script>
  export default {
    name: 'Help',
    computed: {
    }
  }
</script>

<style scoped>

</style>
