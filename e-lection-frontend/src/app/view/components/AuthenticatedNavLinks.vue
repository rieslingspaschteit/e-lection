<script setup lang="ts">
import { ElectionHandler } from '@/app/controller/handler'
import { UserRole } from '@/app/model/user'
import { ConfigProvider } from '@/app/utils/utils'
import { onMounted } from 'vue'
import { ElectionStore } from '../view'

const electionStore = ElectionStore.instance

onMounted(async () => {
  const backendServer = (await ConfigProvider.getInstance().config).backendServer
  new ElectionHandler(backendServer)
    .fetchUserInformation()
    .then(user => {
      electionStore.user = user
      console.log(`fetched User: {
            email: ${user.email}, 
            roles: ${user.roles}
        }`)
    })
    .catch(error => {
      console.log(error)
      Promise.resolve()
    })
})

const isAuthority = () => electionStore.user?.roles
  .includes(UserRole.AUTHORITY)

const isTrustee = () => electionStore.user?.roles
  .includes(UserRole.TRUSTEE)

const signOut = async () => {
  const backendServer = (await ConfigProvider.getInstance().config).backendServer
  window.location.href = backendServer + '/api/logout'
}

</script>

<template>
  <div id="links">
    <router-link to="/verify">
      Verifikation
    </router-link>
    <router-link
      v-if="isTrustee()"
      id="trustee"
      :to="{name: 'Trustee'}"
    >
      Verschlüsselung
    </router-link>
    <router-link
      v-if="isAuthority()"
      id="authority"
      :to="{name: 'Authority'}"
    >
      Wahlen verwalten
    </router-link>
    <button
      id="logout"
      class="btn-prim"
      @click="signOut()"
    >
      Abmelden
    </button>
  </div>
</template>

<style scoped>
a {
    color: white;
    margin: 0 5px 0 5px;
    font-size: 1em;
}

a:hover, .router-link-active {
  border-bottom: 2px solid white;
}
</style>
