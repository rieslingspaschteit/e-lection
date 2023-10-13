<script setup lang="ts">
import { ConfigProvider } from '@/app/utils/utils'
import axios from 'axios'
import { onMounted, ref } from 'vue'

const provider = ref<{
  providerName: string,
  authenticationUrl: string
}[]>()

onMounted(async () => {
  const backendServer = (await ConfigProvider.getInstance().config).backendServer
  axios.get(backendServer + '/api/auth')
    .then(response => {
      provider.value = response.data
    })
})

const signIn = (url : string) => {
  console.log('signing in...')
  window.location.href = url
}

</script>

<template>
  <div id="login-container">
    <b><u>Mit OpenID Provider Anmelden</u></b>
    <div id="provider-container">
      <div
        v-for="prov in provider"
        :id="prov.providerName"
        :key="prov.providerName"
        class="provider"
        @click="signIn(prov.authenticationUrl)"
      >
        <span>{{ prov.providerName }}</span>
        <img
          class="provider-logo"
          :src="'/images/oidc/' + prov.providerName + '.png'"
          onerror="this.src='/images/oidc/fallback.png'"
          alt="oidc provider logo"
        >
      </div>
    </div>
  </div>
</template>

<style scoped>

#login-container {
  margin-top: 50px;
  border: 3px dashed var(--base-border-color);
  border-radius: 8px;
  box-shadow: 0px 0px 10px 2px rgba(0, 0, 0, 0.4);
  padding: 15px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

#provider-container {
  padding-top: 10px;
  width: 100%;
}

.provider {
  margin: 5px;
  border: 1px solid var(--base-border-color);
  padding: 5px;
  padding-right: 1em;
  padding-left: 1em;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.provider:hover {
  cursor: pointer;
  box-shadow: 0px 0px 2px 1px rgba(0, 0, 0, 0.4);
  scale: 1.025;
  transition: scale 0.1s ease-in-out;
}

b {
  font-size: large;
}

.provider-logo {
  width: 1.5em;
}

</style>
