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

          <div class="grid grid-cols-1 xl:grid-cols-4 gap-8">
            <UIFormField>
              <UILabel for="country" required>Country</UILabel>
              <UIDropDown
                required
                placeholder="Select country..."
                id="country"
                v-model="locationCountry"
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
            <UIFormField>
              <UILabel for="city">City</UILabel>
              <UITextInput id="city" placeholder="e.g. Amsterdam" v-model="locationCity" />
            </UIFormField>
          </div>
        </div>

        <div class="space-y-4">
          <h6>Region details</h6>

          <div class="grid grid-cols-1 xl:grid-cols-4 gap-8">
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
                v-model="providerName"
              />
            </UIFormField>
          </div>

          <div class="card__footer form-footer">
            <UIButton ghost variant="neutral" :leading-addon="ArrowLeft" to="/vessel/engine"
              >Back to engine
            </UIButton>
            <UIButton
              :disabled="saveIsPending || deleteIsPending"
              ghost
              variant="destructive"
              v-if="regionId"
              @click="confirmDelete = true"
              >Delete this region
              <LoadingIndicator v-if="deleteIsPending" />
            </UIButton>
            <UIButton :disabled="saveIsPending" type="submit" v-if="regionId"
              >Save this region
              <LoadingIndicator v-if="saveIsPending" />
            </UIButton>
            <UIButton :disabled="saveIsPending" type="submit" v-else :trailing-addon="MapPointAdd"
              >Create region
              <LoadingIndicator v-if="saveIsPending" />
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
    v-if="!!regionId"
    :region-id="regionId"
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
import {
  countries,
  geoRegions,
  providers,
  useDeleteRegion,
  useRegionFormHelpers,
  useSaveRegion,
} from '@/composables/useEngineRegion.ts'
import ConfirmDeleteRegionDialog from '@/components/Dialog/ConfirmDeleteRegionDialog.vue'

const formRef = ref<HTMLFormElement | null>(null)
const confirmDelete = ref(false)

const route = useRoute()
const router = useRouter()
const { regionId } = route.params as { regionId: string | null }

const { name, providerName, geoRegion, locationCity, locationCountry, saveRegion } =
  useRegionFormHelpers()
const { isPending: saveIsPending } = useSaveRegion()
const { isPending: deleteIsPending } = useDeleteRegion()

async function onSubmit() {
  const form = formRef.value!

  form.reportValidity()
  if (!form.checkValidity()) {
    form.reportValidity() // shows browser messages
    return
  }

  await saveRegion()
  await router.push('/vessel/engine')
}

async function onDelete() {
  await router.push('/vessel/engine')
}
</script>
