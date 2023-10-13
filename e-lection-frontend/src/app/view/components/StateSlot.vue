<script setup lang="ts">

defineProps<{
  state?: string
  count?: number
}>()
</script>

<template>
  <div id="state-info">
    <div
      v-if="typeof(count)!=='undefined'"
      id="info"
    >
      Abgeschlossen: {{ count }}
    </div>
    <div id="state">
      {{ state }}
    </div>
  </div>
</template>

<style scoped>
#state-info {
  display: flex;
  font-size: small;
  align-items: center;
}

#state {
  margin-left: 5px;
  background-color: var(--base-border-color-lighter);
  border-radius: 10px;
  padding: 3px;
  color: white;
  font-weight: bold;
}
</style>
