<template>
  <nav class="tab-bar" v-if="nodeId">
    <RouterLink
      class="tab-bar__link"
      exact
      aria-current-value="page"
      :to="`/vessel/${vesselId}/engine/node/${nodeId}`"
      tabindex="0"
    >
      <h6>Node Details</h6>
    </RouterLink>
    <RouterLink
      class="tab-bar__link"
      exact
      aria-current-value="page"
      :to="`/vessel/${vesselId}/engine/node/${nodeId}/server-info`"
      tabindex="0"
    >
      <h6>Server Info</h6>
    </RouterLink>
  </nav>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()
const { nodeId, vesselId } = route.params
</script>
