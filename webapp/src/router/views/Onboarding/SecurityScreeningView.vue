<template>
  <div class="space-y-8 max-w-4xl">
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
          v-model="find"
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
          v-model="role"
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
          v-model="industry"
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
          v-model="users"
          :items="[
            { value: '1', label: 'Solo developer' },
            { value: '1-5', label: '1 - 5 users' },
            { value: '5-10', label: '5 - 10 users' },
            { value: '10-50', label: '10 - 50 users' },
            { value: '50+', label: 'Many, many people' },
          ]"
        />
        <p>
          Based on your users level the app can give you guidance that matches your skills.
        </p>
      </UIFormField>
    </div>
    <div class="form-footer">
      <p>All fields marked with an asterisk (*) are required</p>
      <UIButton :leading-addon="ShieldStar" to="/onboarding/naming-ceremony">Start your Naming Ceremony</UIButton>
    </div>
  </div>
</template>
<script setup lang="ts">
import UIFormField from '@/components/FormField/UIFormField.vue'
import UILabel from '@/components/FormField/UILabel.vue'
import { ShieldStar } from '@solar-icons/vue'
import UIDropDown from '@/components/FormField/UIDropDown.vue'
import { ref } from 'vue'
import UIButton from '@/components/UIButton.vue'

const find = ref('searchengine')
const role = ref('cto')
const industry = ref('')
const users = ref('1')
</script>
