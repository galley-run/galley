<template>
  <div class="space-y-8 max-w-4xl">
    <div class="space-y-2">
      <h1>Naming Ceremony</h1>
      <p class="text-tides-900">
        Here you can provide your company details for billing and client invoicing. Your first
        vessel (cluster) will be named after your business.
      </p>
    </div>

    <div class="grid xl:grid-cols-6 gap-8">
      <UIFormField class="col-span-3">
        <UILabel :required="!billingName" for="billingCompanyName">Company name</UILabel>
        <UITextInput :required="!billingName" id="billingCompanyName" placeholder="e.g. The Yacht Club" v-model="billingCompanyName" />
      </UIFormField>
      <UIFormField class="col-span-3">
        <UILabel required for="billingCountry">Country of origin</UILabel>
        <UIDropDown required id="billingCountry" v-model="billingCountry" :items="countries" />
      </UIFormField>
      <UIFormField class="col-span-6">
        <UILabel :required="!billingCompanyName" for="billingName">Billed to</UILabel>
        <UITextInput :required="!billingCompanyName" id="billingName" placeholder="e.g. Jack Sparrow" v-model="billingName" />
      </UIFormField>
      <UIFormField class="col-span-3">
        <UILabel required for="billingAddress1">Billing Address line 1</UILabel>
        <UITextInput required id="billingAddress1" placeholder="e.g. 1 Waterway" v-model="billingAddress1" />
      </UIFormField>
      <UIFormField class="col-span-3">
        <UILabel for="billingAddress2">Billing Address line 2</UILabel>
        <UITextInput id="billingAddress2" placeholder="e.g. Cabin 4C" v-model="billingAddress2" />
      </UIFormField>
      <UIFormField :class="showState ? 'col-span-2' : 'col-span-3'">
        <UILabel required for="billingPostalCode">Postal Code</UILabel>
        <UITextInput required id="billingPostalCode" placeholder="e.g. 13245" v-model="billingPostalCode" />
      </UIFormField>
      <UIFormField :class="showState ? 'col-span-2' : 'col-span-3'">
        <UILabel required for="billingCity">City</UILabel>
        <UITextInput required id="billingCity" placeholder="e.g. Port of Amsterdam" v-model="billingCity" />
      </UIFormField>
      <UIFormField class="col-span-2" v-if="showState">
        <UILabel for="billingState">State/Province</UILabel>
        <UITextInput id="billingState" placeholder="e.g. California" v-model="billingState" />
      </UIFormField>
      <UIFormField class="col-span-3">
        <UILabel for="billingEmail">Billing email address</UILabel>
        <UITextInput id="billingEmail" placeholder="e.g. captain@yacht.co" v-model="billingEmail" />
      </UIFormField>
      <UIFormField class="col-span-3">
        <UILabel for="billingVAT">VAT number</UILabel>
        <UITextInput id="billingVAT" placeholder="e.g. NL1234156789B01" v-model="billingVAT" />
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
  billingCompanyName: '',
  billingCountry: 'nl',
  billingName: '',
  billingAddress1: '',
  billingAddress2: '',
  billingPostalCode: '',
  billingCity: '',
  billingState: '',
  billingEmail: '',
  billingVAT: '',
})

const {
  billingCompanyName,
  billingCountry,
  billingName,
  billingAddress1,
  billingAddress2,
  billingPostalCode,
  billingCity,
  billingState,
  billingEmail,
  billingVAT,
} = toRefs(state)

const showState = computed(() => {
  const code = (billingCountry.value ?? '').toString().toLowerCase()
  return ['us', 'ca', 'au', 'br', 'mx', 'in', 'cn', 'jp', 'es', 'it', 'gb', 'ie'].includes(code)
})
</script>
