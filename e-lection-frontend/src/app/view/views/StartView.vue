<script setup lang="ts">
import { ElectionHandler } from '@/app/controller/handler'
import { UserRole } from '@/app/model/user'
import { onMounted } from 'vue'
import { ElectionStore } from '../view'
import ElectionItem from '@components/ElectionItem.vue'
import { ElectionState } from '@/app/model/election/states'
import DateSlot from '../components/DateSlot.vue'
import { ConfigProvider } from '@/app/utils/utils'

const store = ElectionStore.instance
let backendServer : string | undefined

onMounted(async () => {
  backendServer = (await ConfigProvider.getInstance().config).backendServer
  await new ElectionHandler(backendServer!)
    .fetchElections(UserRole.VOTER)
    .then(elections => {
      store.voterElections = elections
    })
    .catch(error => {
      console.log(error)
      return Promise.resolve()
    })
  console.log(store.voterElections)
})

const openElections = () => store.voterElections
  .filter(election => election.electionState.name === ElectionState.OPEN.name)

const finishedElections = () => store.voterElections
  .filter(election => election.electionState.name === ElectionState.FINISHED.name ||
election.electionState.name === ElectionState.DECRYPTION.name)
</script>

<template>
  <div id="start-view-container">
    <h2>Offene Wahlen</h2>
    <hr>
    <div class="election-list">
      <ElectionItem
        v-for="election in openElections()"
        :key="election.electionId"
        :election="election"
        name="Dashboard"
        :params="{id: election.electionId, slug: election.electionMeta.title}"
        :redirect="true"
      >
        <DateSlot
          :date="election.electionMeta.end"
          :state="election.electionState"
        />
      </ElectionItem>
    </div>
    <hr>
    <h2>Geschlossene Wahlen</h2>
    <hr>
    <div class="election-list">
      <ElectionItem
        v-for="election in finishedElections()"
        :key="election.electionId"
        :election="election"
        name="Dashboard"
        :params="{id: election.electionId, slug: election.electionMeta.title}"
        :redirect="true"
      >
        <DateSlot
          :date="election.electionMeta.end"
          :state="election.electionState"
        />
      </ElectionItem>
    </div>
    <hr>
  </div>
</template>

<style scoped>
.election-list {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

</style>
