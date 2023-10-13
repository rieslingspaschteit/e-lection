<script setup lang="ts">
import type { TrusteeHandler } from '@/app/controller/handler'
import type { KeyCeremonyElection } from '@/app/model/election'
import JSZip from 'jszip'

const emit = defineEmits(['error'])

const props = defineProps<{
  waiting: boolean
  election: KeyCeremonyElection
  handler?: TrusteeHandler
}>()

// eslint-disable-next-line vue/no-setup-props-destructure
let internElection = props.election

const downloadZipFile = async () => {
  await props.handler?.fetchEBKB()
    .catch(() => {
      emit('error')
      return Promise.resolve()
    })
  await props.handler?.getKeyCeremonyElection()
    .then(ceremonyElection => {
      internElection = ceremonyElection
    })
    .catch(() => emit('error'))

  const obj = internElection.providedEPKB
  const json = JSON.stringify(obj, undefined, ' ')

  const zip = new JSZip()
  zip.file('backup_keys.json', json)

  const content = await zip.generateAsync({ type: 'blob' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(content)
  link.download = 'backup_keys.zip'
  link.click()
}
</script>

<template>
  <h1>Key Ceremony ist abgeschlossen</h1>
  <h3 v-if="!waiting">
    Backup Keys herunterladen
  </h3>
  <button
    @click="downloadZipFile"
  >
    Herunterladen
  </button>
</template>
