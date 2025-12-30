<template>
  <form @submit.prevent="onSubmit" ref="formRef" class="space-y-8 max-w-4xl" novalidate>
    <div class="space-y-2">
      <h1>Naming Ceremony</h1>
      <p class="text-tides-900" v-if="intent === 'business'">
        Here you can provide your company details for billing and client invoicing. Your first
        vessel (cluster) will be named after your business.
      </p>
      <p class="text-tides-900" v-else>
        Let’s name your first vessel (cluster). You can change this later.
      </p>
    </div>

    <div class="grid xl:grid-cols-6 gap-8" v-if="intent === 'business'">
      <UIFormField class="col-span-3">
        <UILabel :required="!vesselBillingProfile.billingTo" for="billingCompanyName"
          >Company name</UILabel
        >
        <UITextInput
          :required="!vesselBillingProfile.billingTo"
          id="billingCompanyName"
          placeholder="e.g. The Yacht Club"
          v-model="vesselBillingProfile.companyName"
        />
        <label for="billingCompanyName" class="form-field__error-message">
          This field is required when <em>Billed to</em> is not given.
        </label>
      </UIFormField>
      <UIFormField class="col-span-3">
        <UILabel required for="billingCountry">Country of origin</UILabel>
        <UIDropDown
          required
          id="billingCountry"
          v-model="vesselBillingProfile.country"
          :items="countries"
        />
        <label for="billingCountry" class="form-field__error-message">
          This field is required.
        </label>
      </UIFormField>
      <UIFormField class="col-span-6">
        <UILabel :required="!vesselBillingProfile.companyName" for="billingTo">Billed to</UILabel>
        <UITextInput
          :required="!vesselBillingProfile.companyName"
          id="billingTo"
          placeholder="e.g. Jack Sparrow"
          v-model="vesselBillingProfile.billingTo"
        />
        <label for="billingTo" class="form-field__error-message">
          This field is required when <em>Company name</em> is not given.
        </label>
      </UIFormField>
      <UIFormField class="col-span-3">
        <UILabel required for="billingAddress1">Billing Address line 1</UILabel>
        <UITextInput
          required
          id="billingAddress1"
          placeholder="e.g. 1 Waterway"
          v-model="vesselBillingProfile.address1"
        />
        <label for="billingAddress1" class="form-field__error-message">
          This field is required.
        </label>
      </UIFormField>
      <UIFormField class="col-span-3">
        <UILabel for="billingAddress2">Billing Address line 2</UILabel>
        <UITextInput
          id="billingAddress2"
          placeholder="e.g. Cabin 4C"
          v-model="vesselBillingProfile.address2"
        />
      </UIFormField>
      <UIFormField :class="showState ? 'col-span-2' : 'col-span-3'">
        <UILabel required for="billingPostalCode">Postal Code</UILabel>
        <UITextInput
          required
          id="billingPostalCode"
          placeholder="e.g. 13245"
          v-model="vesselBillingProfile.postalCode"
        />
        <label for="billingPostalCode" class="form-field__error-message">
          This field is required.
        </label>
      </UIFormField>
      <UIFormField :class="showState ? 'col-span-2' : 'col-span-3'">
        <UILabel required for="billingCity">City</UILabel>
        <UITextInput
          required
          id="billingCity"
          placeholder="e.g. Port of Amsterdam"
          v-model="vesselBillingProfile.city"
        />
        <label for="billingCity" class="form-field__error-message"> This field is required. </label>
      </UIFormField>
      <UIFormField class="col-span-2" v-if="showState">
        <UILabel required for="billingState">State/Province</UILabel>
        <UITextInput
          required
          id="billingState"
          placeholder="e.g. California"
          v-model="vesselBillingProfile.state"
        />
        <label for="billingState" class="form-field__error-message">
          This field is required.
        </label>
      </UIFormField>
      <UIFormField class="col-span-3">
        <UILabel for="billingEmail">Billing email address</UILabel>
        <UITextInput
          id="billingEmail"
          placeholder="e.g. captain@yacht.co"
          v-model="vesselBillingProfile.email"
        />
      </UIFormField>
      <UIFormField class="col-span-3">
        <UILabel for="billingVAT">VAT number</UILabel>
        <UITextInput
          id="billingVAT"
          placeholder="e.g. NL1234156789B01"
          v-model="vesselBillingProfile.vatNumber"
        />
      </UIFormField>
    </div>
    <div v-else>
      <UIFormField>
        <UILabel for="vesselName">Vessel Name</UILabel>
        <UITextInput id="vesselName" placeholder="e.g. Boaty McBoatface" v-model="vessel.name" />
      </UIFormField>
    </div>
    <div class="form-footer justify-between">
      <p>All fields marked with an asterisk (*) are required</p>
      <UIButton type="submit" :leading-addon="ShieldStar"> Set up your first project </UIButton>
    </div>
  </form>
</template>
<script setup lang="ts">
import UIFormField from '@/components/FormField/UIFormField.vue'
import UILabel from '@/components/FormField/UILabel.vue'
import { ShieldStar } from '@solar-icons/vue'
import UIDropDown from '@/components/FormField/UIDropDown.vue'
import { computed, ref } from 'vue'
import UIButton from '@/components/UIButton.vue'
import UITextInput from '@/components/FormField/UITextInput.vue'
import countriesObj from '@/utils/countries.ts'
import { useOnboardingStore } from '@/stores/onboarding.ts'
import { storeToRefs } from 'pinia'
import router from '@/router'

const countries = Object.entries(countriesObj).map(([countryCode, label]) => {
  return { value: countryCode, label }
})

const showState = computed(() => {
  const code = (vesselBillingProfile.value.country ?? '').toString().toLowerCase()
  return ['us', 'ca', 'au', 'br', 'mx', 'in', 'cn', 'jp', 'es', 'it', 'gb', 'ie'].includes(code)
})

const formRef = ref<HTMLFormElement | null>(null)

const onboardingStore = useOnboardingStore()

const { vesselBillingProfile, vessel, intent } = storeToRefs(onboardingStore)

function onSubmit() {
  const form = formRef.value!

  form.reportValidity()
  if (!form.checkValidity()) {
    form.reportValidity() // shows browser messages
    return
  }

  onboardingStore.$patch({
    vesselBillingProfile: vesselBillingProfile.value,
    vessel: {
      name:
        vesselBillingProfile.value.companyName ??
        vesselBillingProfile.value.name ??
        vessel.value.name ??
        'Vessel Inc.',
    },
    charter: {
      name:
        vesselBillingProfile.value.companyName ??
        vesselBillingProfile.value.name ??
        vessel.value.name ??
        'Charter Inc.',
    },
    completed: {
      namingCeremony: true,
    },
  })

  router.push('/onboarding/first-charter')
}
</script>
