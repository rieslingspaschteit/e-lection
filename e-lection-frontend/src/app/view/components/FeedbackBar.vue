<script setup lang="ts">
defineProps<{success: boolean, error: boolean}>()
</script>
<template>
  <div id="feedback">
    <div
      v-if="success"
      id="success-message"
    >
      <slot name="success" />
    </div>
    <div
      v-if="error"
      id="error-message"
    >
      <slot name="error" />
    </div>
  </div>
</template>

<style scoped>

#error-message, #success-message {
  margin-top: 8px;
  border-radius: 5px;
  padding: 10px;
}

#error-message {
  border: 2px solid red;
  background-color: rgb(250, 209, 209);
}

#success-message {
  border: 2px solid lightgreen;
  background-color: rgb(208, 247, 208)}

</style>
