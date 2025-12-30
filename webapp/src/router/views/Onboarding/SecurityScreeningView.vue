<template>
  <form @submit.prevent="onSubmit" ref="formRef" class="space-y-8 max-w-4xl" novalidate>
    <div class="space-y-2">
      <h1>Security Screening</h1>
      <p class="text-tides-900">
        This short survey helps us understand your context: how you discovered Galley, your role,
        the industry you’re in, and how many people you expect to invite. With this we can fine-tune
        your setup and guidance.
      </p>
    </div>

    <div class="grid xl:grid-cols-2 gap-8">
      <UIFormField>
        <UILabel required for="reference">How did you find Galley?</UILabel>
        <UIDropDown
          id="reference"
          required
          v-model="inquiry.reference"
          :items="[
            { value: 'search_engine', label: 'Via Search Engine (e.g. DuckDuckGo, Google, Bing)' },
            { value: 'social_media', label: 'Via Social Media' },
            { value: 'word_of_mouth', label: 'Word of Mouth' },
            { value: 'online_community', label: 'Online Community' },
            { value: 'conference_event', label: 'Conference / Event' },
            { value: 'advertisement', label: 'Advertisement' },
            { value: 'partner_referral', label: 'Partner / Referral' },
            { value: 'blog_article', label: 'Blog / Article' },
            { value: 'other', label: 'Other' },
          ]"
        />
      </UIFormField>
      <UIFormField>
        <UILabel required for="orgRole">What is your role in your organisation?</UILabel>
        <UIDropDown
          id="orgRole"
          required
          placeholder="Select your role..."
          v-model="inquiry.orgRole"
          :items="[
            { value: 'tech_leadership', label: 'CTO / Tech Lead' },
            { value: 'operations', label: 'PO / CPO / CXO / Operations Manager' },
            { value: 'developer', label: 'Software Developer' },
            { value: 'sales', label: 'Sales' },
            { value: 'marketing', label: 'Marketing' },
            { value: 'other', label: 'Other' },
          ]"
        />
      </UIFormField>
      <UIFormField>
        <UILabel required for="orgIndustry">What industry do you work in?</UILabel>
        <UIDropDown
          id="orgIndustry"
          v-model="inquiry.orgIndustry"
          required
          placeholder="Select an industry..."
          :items="[
            { value: 'technology', label: 'IT / Software' },
            { value: 'finance', label: 'Finance' },
            { value: 'healthcare', label: 'Healthcare' },
            { value: 'insurance', label: 'Insurance' },
            { value: 'manufacturing', label: 'Manufacturing' },
            { value: 'retail', label: 'Retail' },
            { value: 'other', label: 'Other' },
          ]"
        />
      </UIFormField>
      <UIFormField>
        <UILabel required for="orgTeamSize">How many people do you expect will use Galley?</UILabel>
        <UIDropDown
          id="orgTeamSize"
          required
          v-model="inquiry.orgTeamSize"
          :items="[
            { value: '1', label: 'Solo developer' },
            { value: '2-5', label: '1 - 5 users' },
            { value: '5-10', label: '5 - 10 users' },
            { value: '10-50', label: '10 - 50 users' },
            { value: '50+', label: 'Many, many people' },
          ]"
        />
        <label for="orgTeamSize">
          Based on your users level the app can give you guidance that matches your skills.
        </label>
      </UIFormField>
    </div>
    <div class="form-footer justify-between">
      <p>All fields marked with an asterisk (*) are required</p>
      <UIButton type="submit" :leading-addon="ShieldStar">Start your Naming Ceremony</UIButton>
    </div>
  </form>
</template>
<script setup lang="ts">
import UIFormField from '@/components/FormField/UIFormField.vue'
import UILabel from '@/components/FormField/UILabel.vue'
import { ShieldStar } from '@solar-icons/vue'
import UIDropDown from '@/components/FormField/UIDropDown.vue'
import { ref } from 'vue'
import UIButton from '@/components/UIButton.vue'
import { useOnboardingStore } from '@/stores/onboarding.ts'
import { storeToRefs } from 'pinia'
import router from '@/router'

const formRef = ref<HTMLFormElement | null>(null)

const onboardingStore = useOnboardingStore()

const { inquiry } = storeToRefs(onboardingStore)

function onSubmit() {
  const form = formRef.value!

  form.reportValidity()
  if (!form.checkValidity()) {
    form.reportValidity() // shows browser messages
    return
  }

  onboardingStore.$patch({
    inquiry: inquiry.value,
    completed: {
      securityScreening: true,
    },
  })

  router.push('/onboarding/naming-ceremony')
}
</script>
