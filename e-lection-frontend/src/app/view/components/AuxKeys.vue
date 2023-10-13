<script setup lang="ts">

import type { TrusteeHandler } from '@/app/controller/handler'
import type { KeyCeremonyElection } from '@/app/model/election'

import { defineEmits } from 'vue'

const emit = defineEmits(['sent', 'error'])

const hasSent = () => {
  emit('sent')
}

const props = defineProps<{
  waiting: boolean
  election: KeyCeremonyElection
  handler?: TrusteeHandler
}>()

const readFile = (fileInput: any) => {
  const file = fileInput.files[0]
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = e => resolve(e.target?.result)
    reader.onerror = e => reject(e)
    reader.readAsText(file)
  })
}

const uploadAuxKey = async (fileInput: any) => {
  let json: object
  await readFile(fileInput)
    .then(content => {
      json = JSON.parse(content as string)
      // eslint-disable-next-line vue/no-mutating-props
      props.election.committedAuxKeys = json
    })
    .catch(error => {
      console.log(error)
      emit('error')
      return Promise.resolve()
    })
  await props.handler?.postKeyCeremony()
    .then(() => hasSent())
    .catch(() => {
      emit('error')
      return Promise.resolve()
    })
}

</script>

<template>
  <h1>Key Ceremony Phase 1</h1>
  <h3 v-if="!waiting">
    Auxiliary Keys hochladen
  </h3>
  <input
    v-if="!waiting"
    id="aux_key"
    ref="auxKey"
    type="file"
    name="aux_key"
    @change="uploadAuxKey($refs.auxKey)"
  >
</template>

<style scoped>

</style>
