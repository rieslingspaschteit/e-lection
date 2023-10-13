<template>
  <footer id="footer">
    <div id="content">
      <span id="header">Ein PSE Project - Version 1.0</span>
      <label
        id="heart-label"
        for="heart"
      >
        Made with
        <i
          id="heart"
          class="fa-solid fa-heart"
        />
        by Paul Enciu, Maximilian Limmer, Christoph Niederbudde, Noah Biwer, Lorenz Heinrich
      </label>
    </div>
  </footer>
</template>

<style scoped>
#footer {
  margin-top: 10px;
  background-color: var(--base-accent-lightest);
  display: flex;
  width: 100%;
  justify-content: center;
  text-align: center;
  bottom: 0
}

#content {
  color: white;
  padding: 5px;
  display: flex;
  flex-direction: column;
  font-size: smaller;
}

</style>
