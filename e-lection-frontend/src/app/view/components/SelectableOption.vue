<script setup lang="ts">
import type { PlainTextBallot } from '@/app/model/ballot'
import { ref } from 'vue'

const props = defineProps<{
  qI: number,
  oI: number,
  optionEntry: [number, string]
  plaintextBallot?: PlainTextBallot
}>()

const selected = ref(props.plaintextBallot?.isSelected(props.qI, props.oI))

const toggle = () => {
  props.plaintextBallot?.toggleSelection(props.qI!, props.oI!)
  selected.value = !selected.value
}

</script>

<template>
  <div class="option">
    <label for="checkable">
      <div
        class="checkable"
        @click="toggle"
      >
        <div
          :class="(selected)?'checked':'unchecked'"
        />
      </div>
      {{ optionEntry[1] }}
    </label>
  </div>
</template>

<style scoped>
.checkable {
  width: 15px;
  height: 15px;
  border: 2px solid var(--base-border-color-lighter);
  margin-right: 10px;
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
}

.checked, .unchecked {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.checked {
  background-color: var(--base-accent-lighter);
}

label {
  margin: 5px;
  display: flex;
}
</style>
