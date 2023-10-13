<script setup lang="ts">
import type { PlainTextBallot } from '@/app/model/ballot'
import SelectableOption from './SelectableOption.vue'

defineProps<{
  title?: string
  plaintextBallot?: PlainTextBallot
  canEdit: boolean
}>()

</script>

<template>
  <div
    id="ballot"
  >
    <h1><u>{{ title }}</u></h1>
    <form
      id="ballot-form"
      :class="{notEditable: !canEdit}"
    >
      <div
        v-for="question in plaintextBallot?.questions.entries()"
        :key="question[0]"
        class="question"
      >
        <div class="title">
          <h3>{{ question[1].questionText }}</h3>
          <span class="max-selections">
            max: {{ question[1].maxSelections }}
          </span>
        </div>
        <hr>
        <SelectableOption
          v-for="option in question[1].options"
          :key="option[0]"
          :q-i="question[0]"
          :o-i="option[0]"
          :option-entry="option"
          :plaintext-ballot="plaintextBallot"
        />
        <hr>
      </div>
    </form>
  </div>
</template>

<style scoped>

.notEditable {
  opacity: 0.4;
  pointer-events: none;
}

#ballot {
  min-width: 400px;
}

.question {
  margin-top: 40px;;
}

.title {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
}

h3 {
  margin: 0;
}

.max-selections {
  font-family: monospace;
  font-size: large;
  padding-left: 10px;
}
</style>
