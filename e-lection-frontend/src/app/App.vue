<script setup lang="ts">
import TheNavbar from '@components/TheNavbar.vue'
import TheFooter from '@components/TheFooter.vue'
</script>
<template>
  <header>
    <TheNavbar />
  </header>
  <article id="content">
    <router-view />
  </article>
  <TheFooter />
</template>

<style scoped>

#content {
  max-width: 800px;
  margin: 0 auto;
  padding: 0 1em 0 1em;
}

</style>
