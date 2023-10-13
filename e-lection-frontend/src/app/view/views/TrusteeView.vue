<script setup lang="ts">
import { ElectionHandler, TrusteeHandler } from '@/app/controller/handler'
import type { Election } from '@/app/model/election'
import { ElectionState } from '@/app/model/election/states'
import { UserRole } from '@/app/model/user'
import { onMounted, ref } from 'vue'
import { ElectionStore } from '../view'
import DateSlot from '../components/DateSlot.vue'
import StateSlot from '../components/StateSlot.vue'
import ElectionItem from '../components/ElectionItem.vue'
import { ConfigProvider } from '@/app/utils/utils'

const store = ElectionStore.instance
let backendServer : string | undefined
const state = ref<Map<number, string>>()
const redirectName = ref<string>()

onMounted(async () => {
  backendServer = (await ConfigProvider.getInstance().config).backendServer
  state.value = new Map()
  await new ElectionHandler(backendServer!)
    .fetchElections(UserRole.TRUSTEE)
    .then(elections => {
      store.trusteeElections = elections
      return elections
    })
    .then(elections => elections
      .forEach(election => getTrusteeDetails(election))
    )
    .catch(error => {
      console.log(error)
      return Promise.resolve()
    })
})

const getTrusteeDetails = async (election : Election) => {
  const handler = new TrusteeHandler(backendServer!, election)
  if (election.electionState === ElectionState.KEY_CEREMONY ||
  election.electionState.name === ElectionState.OPEN.name) {
    await handler.getKeyCeremonyElection()
      .then(election => state.value?.set(election.electionId, election.state))
      .catch(error => {
        console.log(error)
        return Promise.resolve()
      })
    console.log(election.electionState)
    store.setElection(election)
    console.log(store.getElection(election.electionId)?.electionState)
    redirectName.value = 'Key-Ceremony'
  } else if (election.electionState === ElectionState.DECRYPTION) {
    await handler.getDecryptionElection()
      .then(election => state.value?.set(election.electionId, election.state))
      .catch(error => {
        console.log(error)
        return Promise.resolve()
      })
    redirectName.value = 'Decryption'
  } else {
    redirectName.value = 'Dashboard'
  }
  console.log(state.value, (await handler.getKeyCeremonyElection()).state)
  console.log(redirectName.value)
}

const openOrFinished = (election: Election) =>
  election.electionState === ElectionState.OPEN ||
  election.electionState === ElectionState.FINISHED
</script>

<template>
  <div id="trustee-view-container">
    <div id="top">
      <h2>Trustee Wahlen</h2>
    </div>
    <hr>
    <div class="election-list">
      <ElectionItem
        v-for="election in store.trusteeElections"
        :key="election.electionId"
        :params="{id: election.electionId}"
        :election="election"
        :name="redirectName!"
        :slug="election.electionMeta.title"
        :redirect="true"
      >
        <DateSlot
          v-if="openOrFinished(election)"
          :date="election.electionMeta.end"
          :state="election.electionState"
        />
        <StateSlot
          v-else
          :state="state?.get(election.electionId)"
        />
      </ElectionItem>
    </div>
  </div>
</template>
