<template>
  <div class="space-y-8 max-w-4xl">
    <div class="space-y-2">
      <h1>Your First Charter</h1>
      <p class="text-tides-900">
        Galley will provide completely separate namespaces for each of your customers. We’ll call these namespaces Charters.
      </p>
    </div>

    <div class="grid xl:grid-cols-2 gap-8">
      <UIFormField>
        <UILabel required for="charterName">Charter name</UILabel>
        <UITextInput required id="charterName" placeholder="e.g. The Yacht Club" v-model="charterName" />
        <p>Leave blank and we'll use your company name</p>
      </UIFormField>
      <UIFormField>
        <UILabel required for="charterDescription">Charter description</UILabel>
        <UITextInput required id="charterDescription" v-model="charterDescription" />
        <p>Helpful for teams or differentiating between namespaces with similar names.</p>
      </UIFormField>
    </div>
    <div class="form-footer">
      <p>All fields marked with an asterisk (*) are required</p>
      <UIButton :leading-addon="ShieldStar" to="/onboarding/first-charter">
        Set up your first project
      </UIButton>
    </div>
  </div>
</template>
<script setup lang="ts">
import UIFormField from '@/components/FormField/UIFormField.vue'
import UILabel from '@/components/FormField/UILabel.vue'
import { ShieldStar } from '@solar-icons/vue'
import UIDropDown from '@/components/FormField/UIDropDown.vue'
import { computed, reactive, toRefs } from 'vue'
import UIButton from '@/components/UIButton.vue'
import UITextInput from '@/components/FormField/UITextInput.vue'
import countriesObj from '@/utils/countries.ts'

const countries = Object.entries(countriesObj).map(([countryCode, label]) => {
  return { value: countryCode, label }
})

const state = reactive({
  charterName: '',
  charterDescription: '',
  projectName: '',
  projectEnvironment: '',
  projectPurpose: '',
})

const {
  charterName,
  charterDescription,
  projectName,
  projectEnvironment,
  projectPurpose,
} = toRefs(state)

const showState = computed(() => {
  const code = (billingCountry.value ?? '').toString().toLowerCase()
  return ['us', 'ca', 'au', 'br', 'mx', 'in', 'cn', 'jp', 'es', 'it', 'gb', 'ie'].includes(code)
})
</script>
