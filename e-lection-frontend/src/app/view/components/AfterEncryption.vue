<script setup lang="ts">
import SubmittedBallot from './SubmittedBallot.vue'
import { PlainTextBallot, SpoiledBallot as SBallot, type EncryptedBallot } from '@/app/model/ballot'
import { ref } from 'vue'
import SpoiledBallot from './SpoiledBallot.vue'
import { VoterHandler } from '@/app/controller/handler'
import router from '@/app/router/router'
import { ElectionStore } from '../view'
import { globalContext } from '@/app/utils/cryptoConstants'
import type { ElementModQ } from 'electionguard'
import type { ElectionManifest } from '@/app/model/election'
import { ConfigProvider } from '@/app/utils/utils'

const props = defineProps<{
  trackingCode?: string
  lastTrackingCode?: string
  encryptedBallot?: EncryptedBallot,
  nonce: ElementModQ,
  plaintextBallot: PlainTextBallot
}>()

const store = ElectionStore.instance

const decided = ref(false)
const submitted = ref(false)
const spoiled = ref(false)

const spoiledBallot = ref<SBallot>()

const submit = async () => {
  const backendServer = (await ConfigProvider.getInstance().config).backendServer
  await new VoterHandler(backendServer, store.getElection(id()))
    .submitBallot(props.trackingCode!)
    .then(() => {
      submitted.value = true
      decided.value = true
    })
    .catch(error => {
      console.log(error)
      return Promise.resolve(error)
    })
}

const id = () => Number(router.currentRoute.value.params.id)

const spoil = () => {
  spoiledBallot.value = new SBallot(
    globalContext.createElementModQFromHex(props.trackingCode!)!,
    globalContext.createElementModQFromHex(props.lastTrackingCode!)!,
    props.nonce,
    props.encryptedBallot!,
    props.plaintextBallot.getWithDummies()
  )
  spoiledBallot.value.manifest = store.getElection(id())!.electionMeta! as ElectionManifest
  spoiled.value = true
  decided.value = true
}

</script>

<template>
  <div id="after-encryption">
    <p class="centered">
      Die Auswahl wurde erfolgreich verschlüsselt:
    </p>
    <label
      id="tracking-code-label"
      for="tracking-code"
    >Ihr Tracking-Code:</label>
    <div id="tracking-code">
      <span id="code">{{ trackingCode }}</span>
    </div>
    <p class="centered smaller">
      Wir empfehlen, den Tracking-Code vor dem Aufdecken oder Abgeben zu kopieren!
    </p>
    <div id="buttons">
      <button
        :disabled="decided"
        class="btn-prim"
        :class="{disabled : decided}"
        @click="spoil"
      >
        Audecken
      </button>
      <button
        :disabled="decided"
        class="btn-prim"
        :class="{disabled : decided}"
        @click="submit"
      >
        Abgeben
      </button>
    </div>
    <div
      v-if="decided"
      id="after-decision"
    >
      <hr>
      <SpoiledBallot
        v-if="spoiled"
        :spoiled-ballot="spoiledBallot"
      />
      <SubmittedBallot
        v-if="submitted"
      />
    </div>
  </div>
</template>

<style scoped>

#buttons {
  display: flex;
  justify-content: flex-end;
}

button {
  margin: 5px;
}

.centered {
  margin: 0;
  text-align: center;
  margin-bottom: 10px;
}

#after-encryption {
  margin: 15px;
  border: 2px dashed var(--base-border-color);
  padding: 10px;
  border-radius: 8px;
}

.smaller {
  font-size: smaller;
}

.disabled {
  color: black;
  border: 2px solid var(--base-accent-lighter);
  background-color: white;
}

#tracking-code {
  font-family: monospace;
  font-size: large;
  margin: 10px;
  text-align: center;
}

button {
  border: none;
}

</style>
