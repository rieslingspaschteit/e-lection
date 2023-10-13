<script setup lang="ts">
import { ElectionHandler, TrusteeHandler } from '@/app/controller/handler'
import type { DecryptionElection } from '@/app/model/election'
import { DecryptionState } from '@/app/model/election/states'
import router from '@/app/router/router'
import { ConfigProvider } from '@/app/utils/utils'
import JSZip from 'jszip'
import { onMounted, ref } from 'vue'
import FeedbackBar from '../components/FeedbackBar.vue'
import { ElectionStore } from '../view'

const store = ElectionStore.instance
const id = () => Number(router.currentRoute.value.params.id)

let backendServer : string | undefined
const waiting = ref<Boolean>()
const phase = ref<String>()
let handler: TrusteeHandler
let election: DecryptionElection
const error = ref(false)

const downloadZipFile = async () => {
  let result: object | undefined
  const trusteeHandler = new TrusteeHandler(backendServer!, store.getElection(id()))
  await trusteeHandler.fetchEncryptions()
    .catch(error => {
      console.log(error)
    })
  await trusteeHandler
    .getDecryptionElection()
    .then(election => {
      result = election.providedEncryptions
    })
    .catch(error => {
      console.log(error)
      return Promise.reject(error)
    })
  if (result) {
    const json = JSON.stringify(result, undefined, ' ')
    const zip = new JSZip()
    zip.file('encrypted_tallies.json', json)

    const content = await zip.generateAsync({ type: 'blob' })
    const link = document.createElement('a')
    link.href = URL.createObjectURL(content)
    link.download = 'result.zip'
    link.click()
  }
}

const uploadZipFile = async (fileInput: any) => {
  const decryption = await readFile(fileInput)
    .then(content => {
      console.log(`read content: ${content}`)
      return JSON.parse(content as string)
    })
  election.committedDecryptions = decryption
  await handler.postDecryptions()
    .then(() => {
      uploaded.value = true
    })
    .catch(
      errorMessage => {
        error.value = true
        console.log(errorMessage)
      }
    )
}

const uploaded = ref(false)

const readFile = (fileInput: any) => {
  const file = fileInput.files[0]

  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = e => resolve(e.target?.result)
    reader.onerror = e => reject(e)
    reader.readAsText(file)
  })
}

onMounted(async () => {
  backendServer = (await ConfigProvider.getInstance().config).backendServer
  if (!store.getElection(id())) {
    const handler = new ElectionHandler(backendServer)
    await handler
      .fromId(id())
      .then(() => store.setElection(handler.election))
      .catch(error => {
        console.log(error)
      })
  }
  handler = new TrusteeHandler(backendServer, store.getElection(id()))
  await handler.fetchDecryptionState()
  await handler.getDecryptionElection()
    .then(decElection => {
      election = decElection
      waiting.value = election.waiting
      if (election.state === DecryptionState.P_DECRYPTION) {
        phase.value = 'Teilentschlüsseln'
      } else {
        phase.value = 'Teilentschlüsselungen rekonstruieren'
      }
    })
})

const resetError = () => {
  error.value = false
}

</script>

<template>
  <h1>Ergebnis entschlüsseln</h1>
  <h3>Aktuelle Phase: {{ phase }}</h3>
  <h4
    v-if="waiting"
  >
    Teilentschlüsselung wurde hochgeladen
  </h4>
  <h4
    v-if="error"
  >
    Teilentschlüsselung ist ungültig
  </h4>
  <button
    v-if="error"
    @click="resetError"
  >
    OK
  </button>
  <button
    v-if="!waiting&&!error"
    id="result"
    @click="downloadZipFile"
  >
    Ergebnis
  </button>
  <input
    v-if="!waiting&&!error"
    id="decryption-upload"
    ref="Entschlüsselung"
    type="file"
    name="voters"
    @change="uploadZipFile($refs.Entschlüsselung)"
  >
  <FeedbackBar
    :error="error"
    :success="uploaded"
  >
    <template #success>
      Die Teilentschlüsselungen wurden erfolgreich hochgeladen!
    </template>
    <template #error>
      Fehler beim Hochladen der Teilentschlüsselungen...
    </template>
  </FeedbackBar>
</template>
