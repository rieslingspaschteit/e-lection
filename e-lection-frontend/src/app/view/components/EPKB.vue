<script setup lang="ts">
import FeedbackBar from './FeedbackBar.vue'
import { defineEmits, ref } from 'vue'
import type { TrusteeHandler } from '@/app/controller/handler'
import type { KeyCeremonyElection } from '@/app/model/election'

import JSZip from 'jszip'

const emit = defineEmits(['sent', 'error'])

const props = defineProps<{
  waiting: boolean
  election: KeyCeremonyElection
  handler?: TrusteeHandler
}>()

// eslint-disable-next-line vue/no-setup-props-destructure
let internElection = props.election

const readFile = (fileInput: any) => {
  const file = fileInput.files[0]

  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = e => resolve(e.target?.result)
    reader.onerror = e => reject(e)
    reader.readAsText(file)
  })
}

const uploadEpkb = async (fileInput: any) => {
  let json: object
  await readFile(fileInput)
    .then(content => {
      json = JSON.parse(content as string)
      internElection.committedEPKB = json
    })
    .catch(error => {
      emit('error')
      return Promise.resolve(error)
    })
  await props.handler?.postKeyCeremony()
    .then(() => {
      emit('sent')
    })
    .catch(error => {
      emit('error')
      return Promise.resolve(error)
    })
}

const downloadZipFile = async () => {
  let download = true
  await props.handler?.fetchAuxKeys()
    .catch(() => {
      emit('error')
      download = false
      return Promise.resolve()
    })
  await props.handler?.getKeyCeremonyElection()
    .then(ceremonyElection => {
      internElection = ceremonyElection
    })

  const obj = internElection.providedAuxKeys
  const json = JSON.stringify(obj, undefined, ' ')
  if (download) {
    const zip = new JSZip()
    zip.file('aux_keys.json', json)

    const content = await zip.generateAsync({ type: 'blob' })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(content)
    link.download = 'aux_keys.zip'
    link.click()
  }
}

const error = ref(false)
const success = ref(false)

</script>

<template>
  <h1>Key Ceremony Phase 2</h1>
  <h3 v-if="!waiting">
    Auxiliary Keys herunterladen
  </h3>
  <button
    v-if="!waiting"
    id="download-aux-keys"
    @click="downloadZipFile"
  >
    Herunterladen
  </button>
  <h3 v-if="!waiting">
    Öffentlichen Wahlschlüssel und Backups hochladen
  </h3>
  <input
    v-if="!waiting"
    id="epkb"
    ref="epkb"
    type="file"
    name="epkb"
    @change="uploadEpkb($refs.epkb)"
  >
  <FeedbackBar
    :success="success"
    :error="error"
  >
    <template #success>
      Die epkb's wurden erfolgreich hochgeladen
    </template>
    <template #error>
      Fehler beim hochladen der epkb
    </template>
  </FeedbackBar>
</template>

<style scoped>

</style>
