<template>
  <UIDrawer :show="show" @close="pristine && $emit('close')">
    <form @submit.prevent="onSubmit" ref="formRef" novalidate>
      <div class="drawer__header">
        <MapPointWave />
        <div>
          <h3>Add new region</h3>
          <p>Add a new region to your engine.</p>
        </div>
        <UIButton @click="$emit('close')" ghost variant="neutral" :trailing-addon="CloseCircle" />
      </div>
      <div class="drawer__body">
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
      </div>
      <div class="drawer__footer">
        <UIButton ghost variant="neutral" :leading-addon="UndoLeftRound" @click="$emit('close')"
          >Cancel & Close
        </UIButton>
        <UIButton type="submit" :trailing-addon="MapPointAdd"
          >Add region
          <LoadingIndicator v-if="saveRegionMutation.isPending.value" />
        </UIButton>
      </div>
    </form>
  </UIDrawer>
</template>
<script setup lang="ts">
import { CloseCircle, MapPointAdd, MapPointWave, UndoLeftRound } from '@solar-icons/vue'
import UIDrawer from '@/components/Drawer/UIDrawer.vue'
import UIButton from '@/components/UIButton.vue'
import UIFormField from '@/components/FormField/UIFormField.vue'
import UILabel from '@/components/FormField/UILabel.vue'
import UITextInput from '@/components/FormField/UITextInput.vue'
import { ref } from 'vue'
import UIDropDown from '@/components/FormField/UIDropDown.vue'
import UIAutoComplete from '@/components/FormField/UIAutoComplete.vue'
import LoadingIndicator from '@/assets/LoadingIndicator.vue'
import { useRegionForm, geoRegions, countries, providers } from '@/composables/useRegionForm'

const { show } = defineProps<{ show: boolean }>()
const emit = defineEmits<{ (e: 'close'): void; (e: 'select', regionId: string): void }>()

const { name, provider, geoRegion, city, country, pristine, saveRegion, saveRegionMutation } =
  useRegionForm()

const formRef = ref<HTMLFormElement | null>(null)

async function onSubmit() {
  const form = formRef.value!

  form.reportValidity()
  if (!form.checkValidity()) {
    form.reportValidity() // shows browser messages
    return
  }

  const result = await saveRegion()
  emit('select', result.id)
  emit('close')
}
</script>
