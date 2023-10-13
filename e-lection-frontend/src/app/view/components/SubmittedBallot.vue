<template>
  <div id="submitted">
    <div id="success-icon">
      <i class="fa-solid fa-check" />
    </div>
    <div id="success-text">
      Der Stimmzettel wurde abgegeben!
    </div>
    <div id="link-container">
      <router-link
        class="btn-prim"
        :to="{
          name: 'Dashboard',
          params: {
            id: $route.params.id,
            slug: $route.params.slug
          }
        }"
      >
        Dashboard
      </router-link>
    </div>
  </div>
</template>

<style scoped>
#submitted {
  font-size: larger;
  text-align: center;
}

#success-icon {
  color: green;
  font-size: xx-large;
}

#link-container {
  font-size: medium;
  float: right;
}

.btn-prim {
  border-radius: 15px;
}
</style>
