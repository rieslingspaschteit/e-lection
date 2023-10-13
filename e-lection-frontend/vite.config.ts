import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'

import NodeModulesPolyfillPlugin from '@esbuild-plugins/node-modules-polyfill'

// https://vitejs.dev/config/
export default defineConfig({
  optimizeDeps: {
    esbuildOptions: {
      plugins: [NodeModulesPolyfillPlugin()]
    }
  },
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
      '@views': path.resolve(__dirname, 'src/app/view/views'),
      '@components': path.resolve(__dirname, 'src/app/view/components'),
      buffer: require.resolve('rollup-plugin-node-polyfills/polyfills/buffer-es6'),
      stream: require.resolve('rollup-plugin-node-polyfills/polyfills/stream'),
      util: require.resolve('rollup-plugin-node-polyfills/polyfills/util'),
      process: require.resolve('rollup-plugin-node-polyfills/polyfills/process-es6'),
      events: require.resolve('rollup-plugin-node-polyfills/polyfills/events')
    }
  },
  build: {
    sourcemap: true
  }
})
