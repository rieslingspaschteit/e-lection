<script setup lang="ts">
import { ElectionHandler } from '@/app/controller/handler'
import { ElectionState } from '@/app/model/election/states'
import { onBeforeMount } from 'vue'
import { onBeforeRouteUpdate } from 'vue-router'
import { ElectionStore } from '../view'
import ElectionMeta from '@components/ElectionMeta.vue'
import router from '@/app/router/router'
import BallotBoard from '@components/BallotBoard.vue'
import ElectionResult from '@components/ElectionResult.vue'
import type { OpenElection, ElectionManifest, FinishedElection } from '@/app/model/election'
import { ConfigProvider } from '@/app/utils/utils'

const store = ElectionStore.instance
let backendServer : string | undefined
let handler: ElectionHandler

onBeforeMount(async () => {
  backendServer = (await ConfigProvider.getInstance().config).backendServer
  handler = new ElectionHandler(backendServer!)

  await setup()
    .catch(error => {
      console.log(error)
      return Promise.resolve()
    })
})

const getId = () => Number(router.currentRoute.value.params.id)

onBeforeRouteUpdate(async (to, from) => {
  console.log('beforeRouteUpdate: ', router.currentRoute.value.params.id)
  if (to.params.id === from.params.id) return
  await setup()
    .catch(error => {
      console.log(error)
      return Promise.resolve()
    })
})

const setup = async () => {
  return fetchElection()
    .then(() => isFinished()
      ? fetchResultElection()
      : fetchOpenElection()
    )
}

const isFinished = () => {
  return store.getElection(getId())?.electionState.name ===
    ElectionState.FINISHED.name
}

const fetchResultElection = async () => {
  const handler = new ElectionHandler(backendServer!, store.getElection(getId()))
  handler
    .fetchBallot()
    .then(() => handler.fetchTrustees())
    .then(() => handler.getFinishedElection())
    .then(election => store.setElection(election))
}

const fetchOpenElection = async () => {
  handler
    .fetchEligibleToVote()
    .then(() => handler.fetchTrustees())
    .then(() => handler.getOpenElection())
    .then(election => store.setElection(election))
    .then(() => {
      const election = store.getElection(getId())!
      console.log(`fetched:
      trustees: ${(election.electionMeta as ElectionManifest).trustees}
      eligibleToVote: ${(election as OpenElection).eligibleToVote}`
      )
    })
}

const fetchElection = async () => {
  if (!store.getElection(getId())) {
    await handler.fromId(getId())
      .then(() => store.setElection(handler.election))
  } else {
    handler = new ElectionHandler(backendServer!, store.getElection(getId())!)
  }
  console.log(handler.election)
}

</script>

<template>
  <div>
    <h1>{{ store.getElection(getId())?.electionMeta.title }}</h1>
    <label
      id="fingerprint-label"
      for="fingerprint"
    >
      Fingerprint:
      <span id="fingerprint">
        {{ store.getElection(getId())?.fingerprint }}
      </span>
    </label>
    <p id="description">
      {{ store.getElection(getId())?.electionMeta.description }}
    </p>
    <hr>
    <div id="meta">
      <ElectionMeta
        :meta="store.getElection(getId())?.electionMeta"
        :state="store.getElection(getId())?.electionState"
        :trustees="
          (store.getElection(getId())
            ?.electionMeta as ElectionManifest)
            ?.trustees
        "
      />
    </div>
    <hr>
    <div id="sub-content-container">
      <ElectionResult
        v-if="isFinished()"
        :ballot="
          (
            store.getElection(getId())
              ?.electionMeta as ElectionManifest
          )
            .ballot
        "
        :result="
          (store.getElection(getId()) as FinishedElection)
            ?.result
        "
        :handler="handler"
      />
      <BallotBoard
        v-else
        :tracking-codes="
          (store.getElection(getId()) as OpenElection)
            ?.submittedBallots"
        :eligible-to-vote="
          (store.getElection(getId()) as OpenElection)
            ?.eligibleToVote&&store.getElection(getId())?.electionState.name===ElectionState.OPEN.name
        "
      />
    </div>
  </div>
</template>

<style scoped>

#fingerprint {
  font-family: monospace;
  font-weight: normal;
}

#fingerprint-label {
  font-weight: bold;
}

#sub-content-container {
  display: flex;
  justify-content: center;
}

</style>
