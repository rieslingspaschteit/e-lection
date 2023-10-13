<script setup lang="ts">
import { Encryptor } from '@/app/controller/encryption'
import { ElectionHandler, VoterHandler } from '@/app/controller/handler'
import type { EncryptedBallot, PlainTextBallot } from '@/app/model/ballot'
import type { ElectionManifest } from '@/app/model/election'
import router from '@/app/router/router'
import { ConfigProvider } from '@/app/utils/utils'
import { onMounted, ref, type Ref } from 'vue'
import AfterEncryption from '../components/AfterEncryption.vue'
import BallotForm from '../components/BallotForm.vue'
import { ElectionStore } from '../view'

const store = ElectionStore.instance
let backendServer : string | undefined

onMounted(async () => {
  backendServer = (await ConfigProvider.getInstance().config).backendServer

  let election = store.getElection(id())
  if (!election) {
    const handler : ElectionHandler = new ElectionHandler(backendServer)
    await handler
      .fromId(id())
      .then(() => {
        election = handler.election
      })
      .catch(error => {
        console.log(error)
        return Promise.resolve()
      })
  }
  await new VoterHandler(backendServer, election)
    .getVoterElection()
    .then(election => {
      plaintextBallot.value = election.plaintextBallot
      return Promise.resolve(election)
    })
    .then(election => store.setVoterElection(election))
    .catch(error => {
      console.log(error)
      return Promise.resolve()
    })
})

const plaintextBallot : Ref<PlainTextBallot | undefined> = ref()
const encryptedBallot : Ref<EncryptedBallot | undefined> = ref()

const id = () => Number(router.currentRoute.value.params.id)

const encrypted = ref(false)
const beginEncryption = ref(false)
const codes = ref({ trackingCode: '', lastTrackingCode: '' })

const encryptor = new Encryptor()

const encrypt = async () => {
  console.log('encrypt...')
  console.log('attempt to encrypt: ', plaintextBallot.value)
  beginEncryption.value = true
  const handler = new ElectionHandler(backendServer!, store.getElection(id()))

  await handler
    .fetchTrustees()
    .then(() => handler.fetchHashes())
    .then(() => {
      ElectionStore.instance.setElection(handler.election)
      console.log('fetched hashes: ', (handler.election.electionMeta as ElectionManifest).hashes?.stringify())
    })
    .catch(error => {
      console.log(error)
      Promise.reject(error)
    })

  encryptedBallot.value = encryptor.encrypt(plaintextBallot.value!, store.getElection(id())?.electionMeta as ElectionManifest)

  console.log('encrypted ballot: ', encryptedBallot.value.stringify())
  const voterHandler = new VoterHandler(backendServer!, store.getElection(id()))
  await voterHandler
    .postBallot(encryptedBallot.value!)
    .then(trackingCodes => {
      codes.value = trackingCodes
    })
    .then(() => {
      encrypted.value = true
    })
    .catch(error => {
      console.log(error)
      Promise.resolve()
    })
}

</script>

<template>
  <div id="vote-view">
    <BallotForm
      :can-edit="!beginEncryption"
      :title="
        store.getElection(id())?.electionMeta.title
      "
      :plaintext-ballot="plaintextBallot"
    />
    <button
      class="btn-prim"
      :disabled="!plaintextBallot?.check() || beginEncryption"
      :class="{ disabled: !plaintextBallot?.check() || encrypted }"
      @click="encrypt"
    >
      Verschlüsseln
    </button>
    <AfterEncryption
      v-if="encrypted"
      :encrypted-ballot="encryptedBallot"
      :tracking-code="codes.trackingCode"
      :last-tracking-code="codes.lastTrackingCode"
      :nonce="encryptor.getSeed()"
      :plaintext-ballot="plaintextBallot!"
    />
  </div>
</template>

<style scoped>

button {
  border: none;
}

.disabled {
  color: black;
  border: 2px solid var(--base-accent-lighter);
  background-color: white;
}

</style>
