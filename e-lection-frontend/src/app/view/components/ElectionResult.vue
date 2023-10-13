<script setup lang="ts">
import type { ElectionHandler } from '@/app/controller/handler'
import type { Ballot } from '@/app/model/ballot'

const props = defineProps<{
  ballot? : Ballot,
  result?: Map<number, Array<number>>,
  handler: ElectionHandler
}>()

const isWinner = (qI: number, oI: number) =>
  props.result?.get(qI)?.reduce((a, b) => a > b ? a : b) ===
  props.result?.get(qI)?.at(oI)

const sendRecord = async (recordStream: Blob) => {
  console.log('tried fetching record')

  const link = document.createElement('a')
  link.href = URL.createObjectURL(recordStream)
  link.download = 'record.zip'
  link.click()
  console.log('fetched record')
}

const downloadRecord = async () => {
  // FIXME make right
  await props.handler.fetchElectionRecord()
    .catch(error => console.log(error))
  await props.handler.getFinishedElection()
    .then(async election => {
      election.electionRecord
        ? await sendRecord(election.electionRecord)
        : console.log('Record could not be fetched')
    })
    .catch(error => console.log(error))
}
</script>

<template>
  <div id="result">
    <button
      id="record"
      class="btn-prim"
      @click="downloadRecord"
    >
      Record <i class="fa-solid fa-cloud-arrow-down" />
    </button>
    <div
      v-for="question in ballot?.questions.entries()"
      :key="question[0]"
      class="question-container"
    >
      <h3>{{ question[0] }}: {{ question[1].questionText }}:</h3>
      <div id="options-container-wrapper">
        <div id="options-container">
          <hr>
          <div
            v-for="option in question[1].options.entries()"
            :key="option[0]"
            class="option"
            :class="{ winner: isWinner(question[0], option[0]) }"
          >
            <span class="key">{{ (option[0] + 1) }}: {{ option[1] }}</span>
            <span class="value">
              <i
                v-if="isWinner(question[0], option[0])"
                class="fa-solid fa-crown"
              />
              {{ result?.get(question[0])?.at(option[0]) }}
            </span>
            <hr>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>

#options-container-wrapper {
  display: flex;
  justify-content: center;
}

button {
  border: none;
}

button:hover {
  cursor: pointer;
}

.fa-crown {
  color: gold;
}

#result{
  width: 80%;
}

#options-container {
  width: fit-content;
  min-width: 80%;
}

.value {
  float: right;
}

</style>
