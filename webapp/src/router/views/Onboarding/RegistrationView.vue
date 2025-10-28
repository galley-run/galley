<template>
  <form @submit.prevent="onSubmit" ref="formRef" class="space-y-8 max-w-4xl" novalidate>
    <div class="space-y-2">
      <h1>Check-in & Registration</h1>
      <p class="text-tides-900">
        We need a few personal details to create your first user with full access. You’ll also
        choose whether you’re just exploring Galley or setting it up for your business.
      </p>
    </div>
    <div>
      <div class="grid xl:grid-cols-2 gap-8">
        <UIRadioCard
          title="Set up for my business"
          description="You’re ready to board your company. Choose this option and we guide you through all the necessary steps to set up Galley so you can get up and running in no time."
          name="setupMode"
          value="setup"
          v-model="form.setupMode"
        />
        <UIRadioCard
          title="Just explore Galley"
          description="Start your maiden voyage here. We help you to set up  quickly so you can discover how deploying your applications on a Kubernetes* cluster with Galley will help you and your (future) business."
          name="setupMode"
          value="explore"
          v-model="form.setupMode"
        />
      </div>
    </div>

    <div class="grid xl:grid-cols-2 gap-8">
      <UIFormField>
        <UILabel required for="firstName">First name</UILabel>
        <UITextInput
          id="firstName"
          v-model="form.firstName"
          placeholder="e.g. Jack"
          required
          :trailing-addon="UserRounded"
        />
        <label for="firstName" class="form-field__error-message"> This field is required </label>
      </UIFormField>
      <UIFormField>
        <UILabel required for="lastName">Last name</UILabel>
        <UITextInput
          id="lastName"
          v-model="form.lastName"
          placeholder="e.g. Sparrow"
          required
          :trailing-addon="UserRounded"
        />
        <label for="lastName" class="form-field__error-message"> This field is required </label>
      </UIFormField>
      <UIFormField>
        <UILabel required for="email">Email address</UILabel>
        <UITextInput
          id="email"
          v-model="form.email"
          type="email"
          required
          placeholder="e.g. boaty@mcboatface.com"
          :trailing-addon="Letter"
        />
        <label for="email" class="form-field__error-message">
          This field is required and should be a correct email format
        </label>
      </UIFormField>
      <UIFormField>
        <UILabel required for="experience">My technical experience</UILabel>
        <UIDropDown
          id="experience"
          v-model="form.experience"
          required
          :items="[
            { value: 'junior', label: '1 - 3 years' },
            { value: 'medior', label: '3 - 10 years' },
            { value: 'senior', label: '10+ years' },
            { value: 'ancient', label: '20+ years' },
          ]"
        />
        <label for="email" class="form-field__error-message">
          Please select your technical experience level.
        </label>
        <label for="experience">
          Based on your experience level the app can give you guidance that matches your skills.
        </label>
      </UIFormField>
    </div>
    <div class="form-footer">
      <p>All fields marked with an asterisk (*) are required</p>
      <UIButton :leading-addon="Clipboard" type="submit">Enter the Security Screening</UIButton>
    </div>
  </form>
</template>
<script setup lang="ts">
import UIRadioCard from '@/components/FormField/UIRadioCard.vue'
import UIFormField from '@/components/FormField/UIFormField.vue'
import UILabel from '@/components/FormField/UILabel.vue'
import { Clipboard, Letter, UserRounded } from '@solar-icons/vue'
import UITextInput from '@/components/FormField/UITextInput.vue'
import UIDropDown from '@/components/FormField/UIDropDown.vue'
import { reactive, ref } from 'vue'
import UIButton from '@/components/UIButton.vue'
import { useOnboardingStore } from '@/stores/onboarding.ts'

const formRef = ref<HTMLFormElement | null>(null)

const form = reactive({
  setupMode: 'setup' as 'setup' | 'explore',
  firstName: '',
  lastName: '',
  email: '',
  experience: '',
})

const onboardingStore = useOnboardingStore()

function onSubmit() {
  const form = formRef.value!

  form.reportValidity()
  if (!form.checkValidity()) {
    form.reportValidity() // shows browser messages
    return
  }

  onboardingStore.$patch({
    setupMode: form.setupMode,
    user: {
      firstName: form.firstName,
      lastName: form.lastName,
      email: form.email,
      experience: form.experience,
    },
  })
}
</script>
