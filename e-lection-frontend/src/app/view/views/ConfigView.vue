<script setup lang="ts">
import { MutableElectionManifest } from '@/app/model/election'
import { onMounted, ref } from 'vue'
import MetaConfig from '../components/MetaConfig.vue'
import BallotConfig from '@components/BallotConfig.vue'
import { ConfigError } from '@/app/model/error'
import { AuthorityHandler } from '@/app/controller/handler'
import { ElectionStore } from '../view'
import { ConfigProvider } from '@/app/utils/utils'
import FeedbackBar from '../components/FeedbackBar.vue'

const invalid = ref(false)
const created = ref(false)
const store = ElectionStore.instance
let backendServer : string | undefined

const init = ref(false)

onMounted(async () => {
  backendServer = (await ConfigProvider.getInstance().config).backendServer

  store.mutableElectionManifest = new MutableElectionManifest()
  init.value = true
})

const createElection = async () => {
  try {
    const manifest = store.mutableElectionManifest.create()
    await new AuthorityHandler(backendServer!)
      .postElection(manifest)
      .then(() => {
        invalid.value = false
        created.value = true
      })
      .catch(error => {
        console.log(error)
        invalid.value = true
        return Promise.resolve()
      })
  } catch (error : ConfigError | any) {
    if (error instanceof ConfigError) {
      console.log(error.message)
      console.log('incorrect config')
      console.log(store.mutableElectionManifest)
      invalid.value = true
    } else {
      throw error
    }
  }
}

</script>

<template>
  <div
    v-if="init"
    id="config"
  >
    <MetaConfig />
    <hr>
    <BallotConfig />
  </div>
  <hr>
  <div id="create">
    <button
      id="create"
      class="btn-prim"
      :class="{ disabled: created }"
      :disabled="created"
      @click="createElection"
    >
      Wahl erstellen
    </button>
    <FeedbackBar
      :success="created"
      :error="invalid"
    >
      <template #success>
        Die Wahl wurde erfolgreich erstellt
      </template>
      <template #error>
        Fehler beim erstellen der Wahl
      </template>
    </FeedbackBar>
  </div>
</template>

<style scoped>

#error-message, #success-message {
  margin-top: 8px;
  border-radius: 5px;
  padding: 10px;
}

.disabled {
  background-color: white;
  cursor: not-allowed;
  color: black;
  border: 2px solid var(--base-accent-lighter);
}

#error-message {
  border: 2px solid red;
  background-color: rgb(250, 209, 209);
}

#success-message {
  border: 2px solid lightgreen;
  background-color: rgb(208, 247, 208)}

</style>
