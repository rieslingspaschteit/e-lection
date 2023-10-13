<script setup lang="ts">
import { AuthorityHandler, ElectionHandler } from '@/app/controller/handler'
import { UserRole } from '@/app/model/user'
import { onMounted, ref } from 'vue'
import { ElectionStore } from '../view'
import ElectionItem from '../components/ElectionItem.vue'
import StateSlot from '../components/StateSlot.vue'
import DateSlot from '../components/DateSlot.vue'
import type { Election, AuthorityElection } from '@/app/model/election'
import { DecryptionState, ElectionState, KeyCeremonyState } from '@/app/model/election/states'
import { ConfigProvider } from '@/app/utils/utils'

const store = ElectionStore.instance
let backendServer : string | undefined
const openText : string | undefined = 'eröffnen'

onMounted(async () => await init())

const init = async () => {
  backendServer = (await ConfigProvider.getInstance().config).backendServer

  await new ElectionHandler(backendServer!).fetchElections(UserRole.AUTHORITY)
    .then(elections => {
      store.authorityElections = elections
      return Promise.resolve(elections)
    })
    .then(elections => elections
      .filter(election => !openOrFinished(election))
      .forEach(election => getAuthorityDetails(election))
    )
    .catch(error => {
      console.log(error)
      return Promise.resolve()
    })
}

const stateAndCount = ref<Map<number, {state?: string, count?: number}>>(new Map())

const redirect = (election: AuthorityElection) => {
  if (election.electionState.name === ElectionState.KEY_CEREMONY.name) {
    return false
  } else if (election.electionState.name === ElectionState.OPEN.name ||
   election.electionState.name === ElectionState.FINISHED.name) {
    return true
  }
  if (election.electionState.name === ElectionState.DECRYPTION.name) {
    return election.keyCerCount! < election.electionMeta.threshold
  }
  return false
}

const getAuthorityDetails = async (election : Election) => {
  const handler = new AuthorityHandler(backendServer!, election)
  if (election.electionState.name === ElectionState.KEY_CEREMONY.name) {
    await handler.fetchKeyCeremonyAttendance()
      .then(() => stateAndCount.value.set(
        election.electionId,
        {
          state: handler.getAuthorityElection().keyCerState,
          count: handler.getAuthorityElection().keyCerCount
        })
      )
      .catch(error => {
        console.log(error)
        return Promise.resolve()
      })
  } else if (election.electionState.name === ElectionState.DECRYPTION.name) {
    await handler.fetchDecryptionAttendance()
      .then(() => stateAndCount.value.set(
        election.electionId,
        {
          state: handler.getAuthorityElection().decryptionState,
          count: handler.getAuthorityElection().decryptionCount
        })
      )
      .catch(error => {
        console.log(error)
        return Promise.resolve()
      })
  }
  console.log(stateAndCount.value)
}

const openOrFinished = (election: Election) =>
  election.electionState.name === ElectionState.OPEN.name ||
  election.electionState.name === ElectionState.FINISHED.name

const buttonText = (election: Election) => {
  console.log('state and count: ', stateAndCount)
  if (stateAndCount.value.get(election.electionId)?.state === KeyCeremonyState.FINISHED) {
    return openText
  } else if (stateAndCount.value.get(election.electionId)?.state === DecryptionState.P_DECRYPTION &&
  stateAndCount.value.get(election.electionId)!.count! >= election.electionMeta.threshold) {
    return 'weiter' // TODO better text
  }
  return undefined
}

const action = async (args: any[]) => {
  const election = args[0] as Election
  const handler = new AuthorityHandler(backendServer!, election)
  console.log(election.electionState)
  if (election.electionState.name === ElectionState.KEY_CEREMONY.name) {
    await handler.openElection()
      .then(async () => await init())
      .catch(error => {
        console.log(error)
        return Promise.resolve()
      })
  }
  if (election.electionState.name === ElectionState.DECRYPTION.name) {
    await handler.updateDecryption()
      .catch(error => {
        console.log(error)
        return Promise.resolve()
      })
  }
}

</script>

<template>
  <div id="authority-view-container">
    <div id="top">
      <h2>Alle erstellten Wahlen</h2>
      <router-link
        id="create-btn"
        class="btn-prim"
        :to="{name: 'Create'}"
      >
        Neue Wahl erstellen
      </router-link>
    </div>
    <hr>
    <div class="election-list">
      <ElectionItem
        v-for="election in store.authorityElections"
        :key="election.electionId"
        :params="{id: election.electionId, slug: election.electionMeta.title}"
        :election="election"
        name="Dashboard"
        :redirect="redirect(election as AuthorityElection)"
        :text="buttonText(election)"
        @clicked="action"
      >
        <DateSlot
          v-if="openOrFinished(election)"
          :date="election.electionMeta.end"
          :state="election.electionState"
        />
        <StateSlot
          v-else
          :state="stateAndCount?.get(election.electionId)?.state"
          :count="stateAndCount?.get(election.electionId)?.count"
        />
      </ElectionItem>
    </div>
  </div>
</template>

<style scoped>
.election-list {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

#top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.btn-prim {
  border-radius: 15px;
}

</style>
