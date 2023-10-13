<script setup lang="ts">
import { MutableQuestion } from '@/app/model/ballot'
import { onMounted, ref } from 'vue'
import { ElectionStore } from '../view'

const store = ElectionStore.instance

const defaultQuestion = () => {
  const question = new MutableQuestion()
  question.addOption(1)
  question.addOption(2)
  return question
}

const qc = ref(1)
const oc = ref(new Map([[1, 2]]))

const addQuestion = () => {
  qc.value++
  store.mutableElectionManifest.mutableBallot.addQuestion(qc.value, defaultQuestion())
  oc.value.set(qc.value, 2)
}

const removeQuestion = () => {
  store.mutableElectionManifest.mutableBallot.removeQuestion(qc.value)
  qc.value--
}

const addOption = (qi: number) => {
  oc.value.set(qi, oc.value.get(qi)! + 1)
  store.mutableElectionManifest.mutableBallot.getQuestion(qi).addOption(oc.value.get(qi)!)
}

const removeOption = (qi : number) => {
  store.mutableElectionManifest.mutableBallot.getQuestion(qi).removeOption(oc.value.get(qi)!)
  oc.value.set(qi, oc.value.get(qi)! - 1)
}

onMounted(() => {
  store.mutableElectionManifest.mutableBallot.addQuestion(1, defaultQuestion())
})
</script>

<template>
  <div id="ballot-config">
    <div
      v-for="question in store.mutableElectionManifest.mutableBallot.questions"
      :key="question[0]"
    >
      <div class="question">
        <div class="question-text-container">
          <label for="question-text">
            Frage {{ question[0] }}:
            <input
              id="question"
              v-model="question[1].questionText"
              type="text"
              name="question-text"
              class="question-text"
            >
          </label>
        </div>
        <label for="max">
          Max:
          <input
            id="max"
            v-model="question[1].maxSelections"
            type="number"
            name="max"
          >
        </label>
        <div class="options-container">
          <div
            v-for="option in question[1].options"
            :key="option[0]"
            class="option-container"
          >
            <label for="option">
              {{ option[0] }}:
              <input
                id="option"
                v-model="option[1].text"
                type="text"
                name="option"
                class="option-text"
              >
            </label>
          </div>
          <button @click="addOption(question[0])">
            Neue Option
          </button>
          <button
            :disabled="oc.get(question[0]) === 2"
            @click="removeOption(question[0])"
          >
            Option enfernen
          </button>
        </div>
      </div>
      <hr>
    </div>
    <button @click="addQuestion">
      Neue Frage
    </button>
    <button
      :disabled="qc === 1"
      @click="removeQuestion"
    >
      Frage entfernen
    </button>
  </div>
</template>

<style scoped>
.question {
  display: flex;
}

.options-container {
  margin-left: 20px;
}

.option-container {
  margin-bottom: 10px;
}

#max {
  max-width: 40px;
}

.question-text-container {
  margin-right: 10px;
}

</style>
