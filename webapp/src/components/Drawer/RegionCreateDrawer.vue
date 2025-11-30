<template>
  <Drawer :show="show" @close="pristine && $emit('close')">
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
            <UILabel for="city">City</UILabel>
            <UITextInput id="city" placeholder="e.g. Amsterdam" v-model="city" />
          </UIFormField>
          <UIFormField class="col-span-2">
            <UILabel required for="geoRegion">Continent</UILabel>
            <UIDropDown
              placeholder="Select a continent..."
              required
              v-model="geoRegion"
              :items="geoRegions"
            />
          </UIFormField>
          <UIFormField class="col-span-2">
            <UILabel for="name" required>Name</UILabel>
            <UITextInput required id="name" placeholder="e.g. AMS1" v-model="name" />
          </UIFormField>
          <UIFormField class="col-span-2">
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
          <LoadingIndicator v-if="createRegionMutation.isPending.value" />
        </UIButton>
      </div>
    </form>
  </Drawer>
</template>
<script setup lang="ts">
import { CloseCircle, MapPointAdd, MapPointWave, UndoLeftRound } from '@solar-icons/vue'
import Drawer from '@/components/Drawer/Drawer.vue'
import UIButton from '@/components/UIButton.vue'
import UIFormField from '@/components/FormField/UIFormField.vue'
import UILabel from '@/components/FormField/UILabel.vue'
import UITextInput from '@/components/FormField/UITextInput.vue'
import { ref, watch } from 'vue'
import UIDropDown from '@/components/FormField/UIDropDown.vue'
import countriesObj from '@/utils/countries.ts'
import getCityHub from '@/utils/getCityHub.ts'
import UIAutoComplete from '@/components/FormField/UIAutoComplete.vue'
import providersList from '@/utils/providers.ts'
import LoadingIndicator from '@/assets/LoadingIndicator.vue'
import { useMutation, useQuery } from '@tanstack/vue-query'
import axios, { type AxiosResponse } from 'axios'
import type { ApiResponse } from '@/types/api'
import type { EngineRegionSummary } from '@/types/api/engine'
import { useProjectsStore } from '@/stores/projects.ts'
import { storeToRefs } from 'pinia'

const countries = Object.entries(countriesObj).map(([countryCode, country]) => {
  return { value: countryCode, label: country.name }
})

const providers = providersList.map((provider) => {
  return { value: provider, label: provider }
})

const { show } = defineProps<{ show: boolean }>()
const emit = defineEmits<{ (e: 'close'): void; (e: 'select', regionId: string): void }>()

const geoRegions = [
  {
    value: 'eu',
    label: 'Europe',
  },
  {
    value: 'usa',
    label: 'United States',
  },
  {
    value: 'na',
    label: 'North America',
  },
  {
    value: 'latam',
    label: 'Latin America',
  },
  {
    value: 'apac',
    label: 'Asia-Pacific',
  },
  {
    value: 'africa',
    label: 'Africa',
  },
]

const name = ref('')
const provider = ref('')
const geoRegion = ref('')
const city = ref('')
const country = ref('')

const formRef = ref<HTMLFormElement | null>(null)
const pristine = ref(true)

watch([name, provider, geoRegion, city, country], (values) => {
  pristine.value = values.filter(Boolean).length === 0
})

watch(city, (newCity) => {
  const hub = getCityHub(newCity)
  let sequence = 1

  if (engineRegions?.value?.length && hub && engineRegions?.value?.length > 0) {
    const regions = engineRegions.value.filter((region) => region?.attributes?.name?.startsWith(hub))

    if (regions.length > 0) {
      sequence = regions.reduce((acc, region) => Math.max(acc, parseInt(region.attributes?.name?.slice(-1))), 0) + 1
    }
  }

  if (hub && !name.value) {
    name.value = hub + sequence
  }
})

watch(country, (newCountry) => {
  if (newCountry && countriesObj[newCountry]) {
    geoRegion.value = countriesObj[newCountry].region
  }
})

const projectsStore = useProjectsStore()
const { selectedVesselId } = storeToRefs(projectsStore)

const { data: engineRegions } = useQuery({
  enabled: !!selectedVesselId?.value,
  queryKey: ['vessel', selectedVesselId?.value, 'engine', 'regions'],
  queryFn: () =>
    axios.get<ApiResponse<EngineRegionSummary>[], ApiResponse<EngineRegionSummary>[]>(
      `/vessels/${selectedVesselId?.value}/engine/regions`,
    ),
})

const createRegionMutation = useMutation({
  mutationFn: (data: {
    name: string
    locationCity: string
    locationCountry: string
    geoRegion: string
    providerName: string
  }) => axios.post<AxiosResponse<EngineRegionSummary>, AxiosResponse<EngineRegionSummary>>(`/vessels/${selectedVesselId.value}/engine/regions`, data),
  onSuccess: (data) => {
    emit('select', data.id)
    emit('close')
  },
})

async function onSubmit() {
  const form = formRef.value!

  form.reportValidity()
  if (!form.checkValidity()) {
    form.reportValidity() // shows browser messages
    return
  }

  await createRegionMutation.mutateAsync({
    name: name.value,
    locationCity: city.value,
    locationCountry: country.value,
    geoRegion: geoRegion.value,
    providerName: provider.value,
  })
}
</script>
