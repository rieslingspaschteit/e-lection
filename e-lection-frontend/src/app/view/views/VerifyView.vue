<script setup lang="ts">
import { Verifier } from '@/app/controller/encryption'
import type { VerifierResult } from '@/app/controller/encryption/VerifierResult'
import { ElectionHandler } from '@/app/controller/handler'
import { PlainTextBallot, type Ballot } from '@/app/model/ballot'
import type { ElectionManifest } from '@/app/model/election'
import { ConfigProvider } from '@/app/utils/utils'
import type { ElGamalCiphertext } from 'electionguard'
import { ref } from 'vue'
import BallotForm from '../components/BallotForm.vue'

const result = ref<VerifierResult>()
const status = ref(0)
const manifest = ref<ElectionManifest>()
const plaintextBallot = ref<PlainTextBallot>()

const stringifyMap = (encryption: ReadonlyMap<number, ReadonlyMap<number, ElGamalCiphertext>>): string => {
  const arrayMap: Map<number, Array<object>> = new Map<number, Array<object>>()
  for (const question of encryption) {
    arrayMap.set(question[0], [])
    for (const selection of question[1]) {
      const selectionObj = {
        pad: selection[1].pad.toHex(),
        data: selection[1].data.toHex()
      }
        arrayMap.get(question[0])!.push(selectionObj)
    }
  }
  return JSON.stringify(Object.fromEntries(arrayMap), null, 2)
}

const fetchBallot = async (fingerprint: string) => {
  const backendServer = (await ConfigProvider.getInstance().config).backendServer
  const handler = new ElectionHandler(backendServer)
  await handler.fetchElectionByFingerprint(fingerprint)
    .catch(error => {
      console.log(error)
      status.value = 3
      Promise.reject(error)
    })
  await handler.fetchBallot()
    .then(() => {
      manifest.value = handler.election.electionMeta as ElectionManifest
      console.log('success')
    })
    .catch(error => {
      console.log(error)
      status.value = 3
      Promise.reject(error)
    })
}

const readFile = (fileInput: any) => {
  const file = fileInput.files[0]

  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = e => resolve(e.target?.result)
    reader.onerror = e => reject(e)
    reader.readAsText(file)
  })
}

const uploadBallot = async (fileInput: any) => {
  await readFile(fileInput)
    .then(content => {
      console.log(`read content: ${content}`)
      return content
    })
    .then(content => JSON.parse(content as string))
    .then(ballotObj => {
      const verifier = new Verifier(ballotObj)
      result.value = verifier.verify()
    })
    .then(() => console.log(status))
    .catch(error => {
      console.log(error)
      status.value = 2
      console.log('An error occured')
      Promise.resolve()
    })
  await fetchBallot(result.value?.recreatedElectionFingerprint as string)
    .then(() => {
      plaintextBallot.value = PlainTextBallot.fromSelections(manifest.value?.ballot as Ballot,
                result.value?.verifiedSpoiledBallot.plaintextBallot as ReadonlyMap<number, ReadonlyMap<number, number>>)
      status.value = 1
    }).catch(error => {
      console.log(error)
      status.value = 3
      Promise.resolve()
    })
}
</script>

<template>
  <h1 v-if="status==0">
    Spoiled Ballot hochladen
  </h1>
  <input
    v-if="status==0"
    id="voters"
    ref="spoiledBallot"
    type="file"
    name="voters"
    @change="uploadBallot($refs.spoiledBallot)"
  >

  <h2
    v-if="result?.valid && status==1"
    class="green"
  >
    Verifiziert
  </h2>
  <h2
    v-if="!result?.valid && status==1"
    class="red"
  >
    Falsche Verschlüsselung
  </h2>

  <h3 v-if="status==1">
    Fingerprint der Wahl: {{ result?.recreatedElectionFingerprint }}
  </h3>
  <BallotForm
    :can-edit="false"
    :title="manifest?.title"
    :plaintext-ballot="plaintextBallot"
  />
  <h3 v-if="status==1">
    Trackingcode Trackingcode: {{ result?.verifiedSpoiledBallot.trackingCode.toHex() }}
  </h3>
  <h3 v-if="status==1">
    Neu berechneter Trackingcode: {{ result?.recreatedTrackinCode.toHex() }}
  </h3>

  <h4
    v-if="status==1"
    class="columnLeft"
  >
    Verschlüsselung:
  </h4>
  <h4
    v-if="status==1"
    class="columnRight"
  >
    neu berechnete Verschlüsselung:
  </h4>
  <h5
    v-if="status==1"
    class="columnLeft"
  >
    {{
      stringifyMap(result!.verifiedSpoiledBallot.encryptedBallot.ciphertext) }}
  </h5>

  <h5
    v-if="status==1"
    class="columnRight"
  >
    {{
      stringifyMap(result!.reEncryptions) }}
  </h5>

  <h3 v-if="status==2">
    "Ungültiges Format"
  </h3>
  <h3
    v-if="status==3"
    class="red"
  >
    "Wahl wurde nicht gefunden"
  </h3>
</template>

<style scoped>

.red {
  color: #ff0000;
}

.green {
  color: #55ff00;
}
.columnLeft {
  float: left;
  width: 45.00%;
  word-break: break-all;
}

.columnRight {
  float: right;
  width: 45.00%;
  word-break: break-all;
}

</style>
