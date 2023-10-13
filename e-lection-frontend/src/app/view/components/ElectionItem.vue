<script setup lang="ts">
import type { Election } from '@/app/model/election'

const emit = defineEmits(['clicked'])

const props = defineProps<{
  election: Election
  name: string,
  params: {
    id: number,
    slug?: string
  }
  redirect: boolean
  text?: string
}>()

const onClicked = () => {
  emit('clicked', [props.election, props.params.id])
}

</script>

<template>
  <div class="item">
    <div class="information">
      <span class="title">
        {{ election.electionMeta.title }}
      </span>
      <slot />
    </div>
    <router-link
      v-if="redirect"
      :id="`view-election-${params.id}`"
      :to="{
        name: name,
        params: params
      }"
      class="btn-prim"
    >
      ansehen
    </router-link>
    <button
      v-else-if="text"
      class="btn-prim"
      @click="onClicked"
    >
      {{ props.text }}
    </button>
  </div>
</template>

<style scoped>
.item {
  display: flex;
  width: 100%;
  border: 1px solid var(--base-border-color);
  max-width: 600px;
  border-radius: 5px;
  margin: 5px;
  padding: 10px;
  justify-content: space-between;
  box-shadow: 2px 2px 4px rgba(0, 0, 0, 0.4);
}

.information {
  width: 80%;
  display: flex;
  justify-content: space-between;
  margin-right: 10px;
  align-items: center;
}

.title {
  font-weight: bold;
}

@media (max-width: 500px) {
  .item {
    flex-direction: column;
  }
}

</style>
