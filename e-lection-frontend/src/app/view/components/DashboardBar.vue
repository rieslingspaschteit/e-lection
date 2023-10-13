<script setup lang="ts">
import type {
  ElectionState
} from '@/app/model/election/states'

defineProps<{
  eligibleToVote?: boolean
  state?: ElectionState
}>()

</script>

<template>
  <div id="button-bar">
    <router-link
      class="btn-prim"
      :class="{ disabled: !eligibleToVote }"
      :disabled="!eligibleToVote"
      :to="{
        name: 'Vote',
        params:{id: $route.params.id, slug: $route.params.slug}
      }"
    >
      Abstimmen
    </router-link>
  </div>
</template>

<style scoped>
.disabled {
  background-color: white;
  border: 2px solid var(--base-accent-lighter);
  color: black;
}

.disabled:hover {
  cursor: not-allowed;
}
</style>
