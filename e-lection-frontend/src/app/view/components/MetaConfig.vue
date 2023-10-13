<script setup lang="ts">
import { ElectionStore } from '../view'

// const store = ElectionStore.instance
const manifest = ElectionStore.instance.mutableElectionManifest

const readFile = (fileInput: any) => {
  const file = fileInput.files[0]

  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = e => resolve(e.target?.result)
    reader.onerror = e => reject(e)
    reader.readAsText(file)
  })
}

const setVoters = async (fileInput: any) => {
  await readFile(fileInput)
    .then(content => {
      console.log(`read content: ${content}`)
      return content
    })
    .then(content => contentToArray(content))
    .then(voters => {
      manifest.voters = voters
      return Promise.resolve(voters)
    })
    .then(() => console.log(`set voters in Manifest ${manifest.voters}`))
    .catch(error => {
      console.log(error)
      Promise.resolve()
    })
}

const setTrustees = async (fileInput: any) => {
  await readFile(fileInput)
    .then(content => {
      console.log(`read content: ${content}`)
      return content
    })
    .then(content => contentToArray(content))
    .then(trustees => {
      manifest.trustees = trustees
      return Promise.resolve(trustees)
    })
    .then(() => console.log(`set trustees in Manifest ${manifest.trustees}`))
    .catch(error => {
      console.log(error)
      Promise.resolve()
    })
}

const contentToArray = (content: unknown) => {
  if (typeof content !== 'string') {
    throw new Error('file content not a string')
  }
  return [...content.split(',').map(s => s.trim())]
}

</script>

<template>
  <div id="meta-config">
    <h1>Wahl erstellen</h1>
    <form id="config-form">
      <div
        id="title-container"
        class="config-line"
      >
        <label for="title">
          Titel:
          <input
            id="title"
            v-model="manifest.title"
            type="text"
            name="title"
          >
        </label>
      </div>
      <div
        id="description-container"
        class="config-line"
      >
        <label for="description">
          Beschreibung:
          <textarea
            id="description"
            v-model="manifest.description"
            name="description"
            cols="30"
            rows="5"
          />
        </label>
      </div>
      <div
        id="date-container"
        class="config-line"
      >
        <label for="end-date">
          Ende:
          <input
            id="end"
            v-model="manifest.end"
            type="datetime-local"
            name="end"
          >
        </label>
      </div>
      <div
        id="threshold-container"
        class="config-line"
      >
        <label for="threshold">
          Threshold:
          <input
            id="threshold"
            v-model="manifest.threshold"
            type="number"
            name="threshold"
          >
        </label>
      </div>
      <div
        id="voters-container"
        class="config-line"
      >
        <label for="voters">
          Wähler:
          <input
            id="voters"
            ref="voters"
            type="file"
            name="voters"
            @change="setVoters($refs.voters)"
          >
        </label>
      </div>
      <div
        id="trustees-container"
        class="config-line"
      >
        <label for="trustees">
          Trustees:
          <input
            id="trustees"
            ref="trustees"
            type="file"
            name="trustees"
            @change="setTrustees($refs.trustees)"
          >
        </label>
        <label for="bot">
          Bot:
          <input
            id="bot"
            v-model="manifest.isBotEnabled"
            type="checkbox"
            name="bot"
          >
        </label>
      </div>
    </form>
  </div>
</template>

<style scoped>
.config-line {
  margin: 20px;
}
</style>
