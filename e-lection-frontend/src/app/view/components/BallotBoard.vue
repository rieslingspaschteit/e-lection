<script setup lang="ts">

defineProps<{
  eligibleToVote? : boolean
  trackingCodes?: ReadonlyArray<string>
}>()

</script>

<template>
  <div id="ballot-board">
    <div id="top-bar">
      <span id="ballot-board-label">Tracking-Codes:</span>
      <router-link
        v-if="eligibleToVote"
        class="btn-prim"
        :to="{
          name: 'Vote',
          params:{id: $route.params.id, slug: $route.params.slug}
        }"
      >
        Stimme abgeben
      </router-link>
    </div>
    <div id="tracking-codes">
      <p
        v-for="code in trackingCodes"
        :key="code"
      >
        {{ code }}
      </p>
    </div>
  </div>
</template>

<style scoped>
  #tracking-codes {
    margin-top: 5px;
    font-family: monospace;
    text-align: center;
    max-width: 600px;
    padding: 0 10px 0 10px;
    border: 2px dashed var(--base-border-color-lighter);
    border-radius: 10px;
  }

  #ballot-board-label {
    font-weight: bold;
  }

  #top-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .btn-prim {
    border-radius: 20px;
  }

</style>
