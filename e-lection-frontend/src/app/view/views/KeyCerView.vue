<script setup lang="ts">
import { ElectionHandler, TrusteeHandler } from '@/app/controller/handler'
import type { KeyCeremonyElection } from '@/app/model/election'
import { ElectionState, KeyCeremonyState } from '@/app/model/election/states'
import router from '@/app/router/router'
import { onMounted, ref } from 'vue'
import { ElectionStore } from '../view'

import AuxKeys from '../components/AuxKeys.vue'
import EPKB from '../components/EPKB.vue'
import FinishedKeyCeremony from '../components/FinishedKeyCeremony.vue'
import { ConfigProvider } from '@/app/utils/utils'

let backendServer : string | undefined
const store = ElectionStore.instance
const id = () => Number(router.currentRoute.value.params.id)
let handler : TrusteeHandler | undefined
const ceremonyState = ref<KeyCeremonyState>()
const ceremonyElection = ref<KeyCeremonyElection>()
const waiting = ref(false)
const failure = ref(0)

onMounted(async () => {
  backendServer = (await ConfigProvider.getInstance().config).backendServer
  handler = new TrusteeHandler(backendServer)
  let election = store.getElection(id())
  if (!election) {
    const basicHandler = new ElectionHandler(backendServer)
    await basicHandler
      .fromId(id())
      .then(() => {
        election = basicHandler.election
      })
      .catch(error => {
        console.log(error)
        console.log('election fetch', id())
        failure.value = 1
        return Promise.resolve()
      })
  }
  if (election?.electionState.name !== ElectionState.KEY_CEREMONY.name &&
  election?.electionState.name !== ElectionState.OPEN.name) {
    console.log(election?.electionState)
    failure.value = 2
  }
  handler = new TrusteeHandler(backendServer, election)
  await refreshState()
    .catch(() => {
      failure.value = 1
      return Promise.resolve()
    })
  console.log(ceremonyState.value, waiting.value, failure.value)
})

const refreshState = async () => {
  console.log('refreshed state')
  await handler?.fetchKeyCeremony()
  await handler?.getKeyCeremonyElection()
    .then(election => {
      ceremonyElection.value = election
      waiting.value = ceremonyElection.value!.waiting
    })
    .catch(error => {
      failure.value = 1
      return Promise.resolve(error)
    })
  ceremonyState.value = ceremonyElection.value?.state
}

const clear = () => {
  failure.value = 0
}

const setError = () => {
  failure.value = 1
}

</script>

<template>
  <AuxKeys
    v-if="ceremonyState==KeyCeremonyState.AUX_KEYS && failure==0"
    :waiting="waiting"
    :election="ceremonyElection!"
    :handler="handler"
    @sent="refreshState"
    @error="setError"
  />

  <EPKB
    v-if="ceremonyState==KeyCeremonyState.EPKB && failure==0"
    :waiting="waiting"
    :election="ceremonyElection!"
    :handler="handler"
    @sent="refreshState"
    @error="setError"
  />

  <FinishedKeyCeremony
    v-if="ceremonyState==KeyCeremonyState.FINISHED && failure==0"
    :waiting="waiting"
    :election="ceremonyElection!"
    :handler="handler"
    @error="setError"
  />

  <h2 v-if="waiting">
    Alle Schlüssel für die aktuelle Phase abgegeben
  </h2>

  <h2 v-if="failure==1">
    Anfrage fehlgeschlagen
  </h2>
  <button
    v-if="failure==1"
    @click="clear"
  >
    OK
  </button>
</template>
