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
          required
          value="setup"
          v-model="setupMode"
        />
        <UIRadioCard
          title="Just explore Galley"
          description="Start your maiden voyage here. We help you to set up  quickly so you can discover how deploying your applications on a Kubernetes* cluster with Galley will help you and your (future) business."
          required
          name="setupMode"
          value="explore"
          v-model="setupMode"
        />
      </div>
    </div>

    <div class="grid xl:grid-cols-2 gap-8">
      <UIFormField>
        <UILabel required for="firstName">First name</UILabel>
        <UITextInput
          id="firstName"
          v-model="user.firstName"
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
          v-model="user.lastName"
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
          v-model="user.email"
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
          v-model="user.experience"
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
import { ref } from 'vue'
import UIButton from '@/components/UIButton.vue'
import { useOnboardingStore } from '@/stores/onboarding.ts'
import router from '@/router'
import { storeToRefs } from 'pinia'

const formRef = ref<HTMLFormElement | null>(null)

const onboardingStore = useOnboardingStore()

const { user, setupMode } = storeToRefs(onboardingStore)

function onSubmit() {
  const form = formRef.value!

  form.reportValidity()
  if (!form.checkValidity()) {
    form.reportValidity() // shows browser messages
    return
  }

  onboardingStore.$patch({
    setupMode: setupMode.value,
    user: user.value,
  })

  router.push('/onboarding/security-screening')
}
</script>
