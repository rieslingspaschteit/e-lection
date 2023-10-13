<script setup lang="ts">
import type { ElectionMeta } from '@/app/model/election'
import type { ElectionState } from '@/app/model/election/states'

defineProps<{
  meta? : ElectionMeta
  state? : ElectionState
  trustees?: ReadonlyArray<string>
}>()

</script>

<template>
  <div id="meta-container">
    <div id="meta-information">
      <div class="entry">
        <span class="key">Wahlleitung:</span>
        <span class="value">{{ meta?.authority }}</span>
      </div>
      <div class="entry">
        <span class="key">Zeitraum:</span>
        <span class="value">
          {{ meta?.start?.toLocaleString() }}
          -
          {{ meta?.end.toLocaleString() }}
        </span>
      </div>
      <div class="entry">
        <span
          id="election-state-key"
          class="key"
        >Status:</span>
        <span
          id="election-state-value"
          class="value"
        >{{ state?.name }}</span>
      </div>
    </div>
    <div id="trustees-container">
      <label
        id="trustees-label"
        for="trustees"
      >
        Trustees:
        <div id="trustees">
          <span
            v-for="trustee in trustees"
            :key="trustee"
            class="trustee"
          >
            {{ trustee + ", " }}
          </span>
        </div>
      </label>
    </div>
  </div>
</template>

<style scoped>
#meta-container {
  display: flex;
  align-items: center;
}

#meta-information, #trustees-container {
  width: 50%;
}

.entry {
  margin: 5px 0 5px 0;
}

.value {
  float: right;
}

.key {
  font-weight: bold;
}

.trustee {
  font-weight: normal;
}

#trustees {
  display: flex;
  flex-wrap: wrap;
  margin-left:25px;
  padding: 5px;
  border: 2px dashed var(--base-border-color-lighter);
  border-radius: 10px;
}

#trustees-label {
  font-weight: bold;
  margin-left: 10px;
}
</style>
