<script setup lang="ts">
import { ElectionStore } from '../view'
import AuthenticatedNavLinks from './AuthenticatedNavLinks.vue'

const store = ElectionStore.instance

</script>
<template>
  <div id="nav-bar">
    <router-link to="/">
      <div class="logo">
        <img
          src="/images/e-lection_logo.png"
          alt="e-lection logo"
        >
      </div>
    </router-link>
    <AuthenticatedNavLinks v-if="store.authenticated" />
  </div>
</template>

<style scoped>
  #nav-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    background-color: var(--base-accent-darker);
    padding: 10px;
    height: fit-content;
  }

  img {
    width: 150px;
  }

</style>
