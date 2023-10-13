<script setup lang="ts">
import type { SpoiledBallot } from '@/app/model/ballot'
import JSZip from 'jszip'

const props = defineProps<{
  spoiledBallot?: SpoiledBallot
}>()

const getFileName = () =>
  props.spoiledBallot!.trackingCode.toHex().slice(0, 5).concat('.json')

const downloadZiFile = async () => {
  const obj = props.spoiledBallot?.stringify()
  const json = JSON.stringify(obj, undefined, ' ')

  const zip = new JSZip()
  zip.file(getFileName(), json)

  const content = await zip.generateAsync({ type: 'blob' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(content)
  link.download = 'spoiled-ballot.zip'
  link.click()
}
</script>

<template>
  <div id="spoiled-container">
    <p>Alle Information über das Ballot wurden in die JSON Datei geschrieben</p>
    <p>Für eine aussagekräftige Verifizierung verwenden Sie bitte in anderes Gerät</p>
    <div id="spoiled-ballot">
      <div id="json">
        <img
          src="/images/json.png"
          alt=""
        >
      </div>
      <button
        class="btn-prim"
        @click="downloadZiFile"
      >
        Herunterladen
      </button>
    </div>
  </div>
</template>

<style scoped>
button {
  border: none;
}

#spoiled-ballot {
  display: flex;
  justify-content: center;
  align-items: center;
}

p {
  text-align: center;
}

#json {
  margin: 10px;
}
</style>
