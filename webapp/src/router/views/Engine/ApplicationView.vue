<script setup lang="ts">
import { AddCircle } from '@solar-icons/vue'
import UIButton from '@/components/UIButton.vue'
</script>
<template>
  <div class="flex flex-col gap-8">
    <div class="card">
      <div class="card__header">
        <h1>Applications</h1>
        <UIButton
          ghost
          :leading-addon="AddCircle"
          title="Add application"
          :to="`/application/add`"
        />
      </div>
      <p>Here you can configure the engine of your platform for your tenants.</p>
    </div>
  </div>
</template>
