<script setup lang="ts">
import { ElectionState } from '@/app/model/election/states'

const props = defineProps<{
  state?: ElectionState
  date?: Date
}>()
const getDateDescription = () => {
  return props.state?.name === ElectionState.OPEN.name
    ? 'Offen bis:'
    : 'Geschlossen seit:'
}
</script>
<template>
  <span class="due-date">
    {{ getDateDescription() }}
    {{ date?.toLocaleDateString() }}
  </span>
</template>

<style scoped>
.due-date {
  font-size: 0.8em;
}
</style>
