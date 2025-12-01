<template>
  <form @submit.prevent="onSubmit" class="flex flex-col gap-8" ref="formRef" novalidate>
    <div class="card">
      <h1 v-if="regionId">Edit this region</h1>
      <h1 v-else>Add new region</h1>
      <p class="mb-6">
        This configuration can be used to bind applications and/or databases to. You can change
        billing settings later, however once the configuration is set. You can’t edit it for
        applications deployed with this virtual server.
      </p>

      <div class="space-y-8">
        <div class="space-y-4">
          <h6>Geographic region</h6>

          <div class="grid grid-cols-2 gap-8">
            <UIFormField>
              <UILabel for="country" required>Country</UILabel>
              <UIDropDown
                required
                placeholder="Select country..."
                id="country"
                v-model="country"
                :items="countries"
              />
            </UIFormField>
            <UIFormField>
              <UILabel required for="geoRegion">Continent</UILabel>
              <UIDropDown
                placeholder="Select a continent..."
                required
                v-model="geoRegion"
                :items="geoRegions"
              />
            </UIFormField>
            <UIFormField class="col-span-2">
              <UILabel for="city">City</UILabel>
              <UITextInput id="city" placeholder="e.g. Amsterdam" v-model="city" />
            </UIFormField>
          </div>
        </div>

        <div class="space-y-4">
          <h6>Region details</h6>

          <div class="grid grid-cols-2 gap-8">
            <UIFormField>
              <UILabel for="name" required>Name</UILabel>
              <UITextInput required id="name" placeholder="e.g. AMS1" v-model="name" />
            </UIFormField>
            <UIFormField>
              <UILabel for="provider">Provider</UILabel>
              <UIAutoComplete
                id="provider"
                :items="providers"
                placeholder="e.g. Hetzner"
                v-model="provider"
              />
            </UIFormField>
          </div>

          <div class="card__footer form-footer">
            <UIButton ghost variant="neutral" :leading-addon="ArrowLeft" to="/vessel/engine"
              >Back to engine
            </UIButton>
            <UIButton
              :disabled="saveRegionMutation.isPending.value || deleteRegionMutation.isPending.value"
              ghost
              variant="destructive"
              v-if="regionId"
              @click="confirmDelete = true"
              >Delete this region
              <LoadingIndicator v-if="deleteRegionMutation.isPending.value" />
            </UIButton>
            <UIButton :disabled="saveRegionMutation.isPending.value" type="submit" v-if="regionId"
              >Save this region
              <LoadingIndicator v-if="saveRegionMutation.isPending.value" />
            </UIButton>
            <UIButton
              :disabled="saveRegionMutation.isPending.value"
              type="submit"
              v-else
              :trailing-addon="MapPointAdd"
              >Create region
              <LoadingIndicator v-if="saveRegionMutation.isPending.value" />
            </UIButton>
          </div>
        </div>
      </div>
    </div>
  </form>
  <ConfirmDeleteRegionDialog
    :show="confirmDelete"
    @close="confirmDelete = false"
    @confirm="onDelete"
  />
</template>
<script setup lang="ts">
import UIDropDown from '@/components/FormField/UIDropDown.vue'
import UILabel from '@/components/FormField/UILabel.vue'
import UIFormField from '@/components/FormField/UIFormField.vue'
import UITextInput from '@/components/FormField/UITextInput.vue'
import { ref } from 'vue'
import UIButton from '@/components/UIButton.vue'
import LoadingIndicator from '@/assets/LoadingIndicator.vue'
import { ArrowLeft, MapPointAdd } from '@solar-icons/vue'
import { useRoute, useRouter } from 'vue-router'
import UIAutoComplete from '@/components/FormField/UIAutoComplete.vue'
import { countries, geoRegions, providers, useRegionForm } from '@/composables/useRegionForm'
import ConfirmDeleteRegionDialog from '@/components/Dialog/ConfirmDeleteRegionDialog.vue'

const formRef = ref<HTMLFormElement | null>(null)
const confirmDelete = ref(false)

const route = useRoute()
const router = useRouter()
const { regionId } = route.params

const {
  name,
  provider,
  geoRegion,
  city,
  country,
  saveRegion,
  saveRegionMutation,
  deleteRegion,
  deleteRegionMutation,
} = useRegionForm()

async function onSubmit() {
  const form = formRef.value!

  form.reportValidity()
  if (!form.checkValidity()) {
    form.reportValidity() // shows browser messages
    return
  }

  await saveRegion(regionId as string | undefined)
  await router.push('/vessel/engine')
}

async function onDelete() {
  if (!regionId || typeof regionId !== 'string') return

  await deleteRegion(regionId)
  await router.push('/vessel/engine')
}
</script>
