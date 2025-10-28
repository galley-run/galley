<template>
  <div class="space-y-8 max-w-4xl">
    <div class="space-y-2">
      <h1>Your First Charter</h1>
      <p class="text-tides-900">
        Galley will provide completely separate namespaces for each of your customers. We’ll call
        these namespaces Charters.
      </p>
    </div>

    <div class="grid xl:grid-cols-2 gap-8">
      <UIFormField>
        <UILabel required for="charterName">Charter name</UILabel>
        <UITextInput
          required
          id="charterName"
          placeholder="e.g. The Yacht Club"
          v-model="charterName"
        />
        <p>Leave blank and we'll use your company name</p>
      </UIFormField>
      <UIFormField>
        <UILabel required for="charterDescription">Description</UILabel>
        <UITextInput required id="charterDescription" v-model="charterDescription" />
        <p>Helpful for teams or differentiating between namespaces with similar names.</p>
      </UIFormField>
    </div>
    <SlashesDivider class="opacity-30" />
    <div class="space-y-2">
      <h4 class="text-navy-700">First project for this charter</h4>
      <p class="text-tides-900">
        Any charter will have one or more projects. A project will contain your apps and databases.
        This way you can combine and separate concerns wherever you need.
      </p>
    </div>
    <div class="grid xl:grid-cols-2 gap-8">
      <UIFormField>
        <UILabel required for="projectEnvironment">Environment type</UILabel>
        <UIDropDown
          required
          id="projectEnvironment"
          v-model="projectEnvironment"
          :items="[
            { value: 'production', label: 'Production' },
            { value: 'staging', label: 'Staging' },
            { value: 'test', label: 'Test' },
            { value: 'development', label: 'Development' },
          ]"
        />
      </UIFormField>
      <UIFormField>
        <UILabel required for="projectName">Project name</UILabel>
        <UITextInput
          required
          id="projectName"
          :placeholder="projectNamePlaceholder"
          v-model="projectName"
        />
        <p>We recommend using the domain name where you will deploy to as Project name.</p>
      </UIFormField>
      <UIFormField class="col-span-2">
        <UILabel required for="projectPurpose">Tell us what it’s for</UILabel>
        <UIDropDown
          required
          id="projectPurpose"
          v-model="projectPurpose"
          :items="[
            { value: 'spa', label: 'Single-Page Application' },
            { value: 'webapp', label: 'Web Application' },
            { value: 'fullstack', label: 'Full Stack Application' },
            { value: 'api', label: 'API Platform' },
            { value: 'demo', label: 'Just trying out Galley' },
            ]"
        />
      </UIFormField>
    </div>
    <div class="form-footer">
      <p>All fields marked with an asterisk (*) are required</p>
      <UIButton :leading-addon="SuitcaseTag" to="/onboarding/boarding">
        Start your journey with Galley
      </UIButton>
    </div>
  </div>
</template>
<script setup lang="ts">
import UIFormField from '@/components/FormField/UIFormField.vue'
import UILabel from '@/components/FormField/UILabel.vue'
import { SuitcaseTag } from '@solar-icons/vue'
import { computed, reactive, toRefs } from 'vue'
import UIButton from '@/components/UIButton.vue'
import UITextInput from '@/components/FormField/UITextInput.vue'
import SlashesDivider from '@/assets/SlashesDivider.vue'
import UIDropDown from '@/components/FormField/UIDropDown.vue'

const state = reactive({
  charterName: '',
  charterDescription: '',
  projectName: '',
  projectEnvironment: 'production',
  projectPurpose: 'webapp',
})

const { charterName, charterDescription, projectName, projectEnvironment, projectPurpose } =
  toRefs(state)

const projectNamePlaceholder = computed(() => {
  switch (projectEnvironment.value) {
    case 'staging':
      return 'e.g. staging.galley.run'
    case 'test':
      return 'e.g. test.galley.run'
    case 'development':
      return 'e.g. dev.galley.run'
    default:
      return 'e.g. galley.run'
  }
})
</script>
