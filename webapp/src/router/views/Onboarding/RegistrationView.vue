<template>
  <div class="space-y-8 max-w-4xl">
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
          name="option"
          value="setup"
          checked
        />
        <UIRadioCard
          title="Just explore Galley"
          description="Start your maiden voyage here. We help you to set up  quickly so you can discover how deploying your applications on a Kubernetes* cluster with Galley will help you and your (future) business."
          name="option"
          value="explore"
        />
      </div>
    </div>

    <div class="grid xl:grid-cols-2 gap-8">
      <UIFormField>
        <UILabel required for="firstName">First name</UILabel>
        <UITextInput
          id="firstName"
          v-model="firstName"
          placeholder="e.g. Jack"
          :trailing-addon="UserRounded"
        />
      </UIFormField>
      <UIFormField>
        <UILabel required for="lastName">Last name</UILabel>
        <UITextInput
          id="lastName"
          v-model="lastName"
          placeholder="e.g. Sparrow"
          :trailing-addon="UserRounded"
        />
      </UIFormField>
      <UIFormField>
        <UILabel required for="email">Email address</UILabel>
        <UITextInput
          id="email"
          v-model="email"
          type="email"
          placeholder="e.g. boaty@mcboatface.com"
          :trailing-addon="Letter"
        />
      </UIFormField>
      <UIFormField>
        <UILabel required for="experience">My technical experience</UILabel>
        <UIDropDown
          id="experience"
          v-model="experience"
          :items="[
            { value: 'beginner', label: '1 - 5 years' },
            { value: 'intermediate', label: '5+ years' },
            { value: 'advanced', label: '10+ years' },
          ]"
        />
        <p>
          Based on your experience level the app can give you guidance that matches your skills.
        </p>
      </UIFormField>
    </div>
    <div class="form-footer">
      <p>All fields marked with an asterisk (*) are required</p>
      <UIButton :leading-addon="Clipboard" to="/onboarding/security-screening">Enter the Security Screening</UIButton>
    </div>
  </div>
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

const firstName = ref('')
const lastName = ref('')
const email = ref('')
const experience = ref('')
</script>
