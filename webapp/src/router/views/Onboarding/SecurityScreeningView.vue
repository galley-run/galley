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
        <UILabel required for="find">How did you find Galley?</UILabel>
        <UIDropDown
          id="find"
          v-model="questionnaire.find"
          :items="[
            { value: 'searchengine', label: 'Via Search Engine (e.g. DuckDuckGo, Google, Bing)' },
            { value: 'socialmedia', label: 'Via Social Media' },
            { value: 'other', label: 'Other' },
          ]"
        />
      </UIFormField>
      <UIFormField>
        <UILabel required for="role">What is your role in your organisation?</UILabel>
        <UIDropDown
          id="role"
          v-model="questionnaire.role"
          :items="[
            { value: 'cto', label: 'CTO / Tech Lead' },
            { value: 'cxo', label: 'Non-technical CXO' },
            { value: 'developer', label: 'Software Developer' },
            { value: 'devops', label: 'DevOps Developer' },
            { value: 'qa', label: 'QA / Test Engineer' },
            { value: 'other', label: 'Other' },
          ]"
        />
      </UIFormField>
      <UIFormField>
        <UILabel required for="industry">What industry do you work in?</UILabel>
        <UIDropDown
          id="industry"
          v-model="questionnaire.industry"
          placeholder="Select an industry..."
          :items="[
            { value: 'saas', label: 'SaaS' },
            { value: 'edtech', label: 'EdTech' },
            { value: 'nonprofit', label: 'Non-profit' },
            { value: 'consulting', label: 'Consulting' },
            { value: 'other', label: 'Other' },
          ]"
        />
      </UIFormField>
      <UIFormField>
        <UILabel required for="users">How many people do you expect will use Galley?</UILabel>
        <UIDropDown
          id="users"
          v-model="questionnaire.users"
          :items="[
            { value: '1', label: 'Solo developer' },
            { value: '1-5', label: '1 - 5 users' },
            { value: '5-10', label: '5 - 10 users' },
            { value: '10-50', label: '10 - 50 users' },
            { value: '50+', label: 'Many, many people' },
          ]"
        />
        <label for="users">
          Based on your users level the app can give you guidance that matches your skills.
        </label>
      </UIFormField>
    </div>
    <div class="form-footer">
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

const { questionnaire } = storeToRefs(onboardingStore)

function onSubmit() {
  const form = formRef.value!

  form.reportValidity()
  if (!form.checkValidity()) {
    form.reportValidity() // shows browser messages
    return
  }

  onboardingStore.$patch({
    questionnaire: questionnaire.value,
  })

  router.push('/onboarding/naming-ceremony')
}
</script>
