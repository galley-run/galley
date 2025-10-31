<template>
  <form @submit.prevent="onSubmit" ref="formRef" class="space-y-8 max-w-4xl" novalidate>
    <div class="space-y-2">
      <h1>Your First Charter</h1>
      <p class="text-tides-900">
        Galley will provide completely separate namespaces for each of your customers. We’ll call
        these namespaces Charters.
      </p>
    </div>

    <div class="grid xl:grid-cols-2 gap-8">
      <UIFormField>
        <UILabel required for="charterName">Name</UILabel>
        <UITextInput
          required
          id="charterName"
          placeholder="e.g. The Yacht Club"
          v-model="charter.name"
        />
        <label for="charterName" class="form-field__error-message"> This field is required. </label>
        <label for="charterName"> We've already named your first namespace after your company. </label>
      </UIFormField>
      <UIFormField>
        <UILabel for="charterDescription">Description</UILabel>
        <UITextInput
          id="charterDescription"
          v-model="charter.description"
          placeholder="e.g. The company's blog"
        />
        <label for="charterDescription"
          >Helpful for teams or differentiating between namespaces with similar names.</label
        >
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
        <label for="projectEnvironment" class="form-field__error-message">
          This field is required.
        </label>
      </UIFormField>
      <UIFormField>
        <UILabel required for="projectName">Project name</UILabel>
        <UITextInput
          required
          id="projectName"
          :placeholder="projectNamePlaceholder"
          v-model="projectName"
        />
        <label for="projectName" class="form-field__error-message"> This field is required. </label>
        <label for="projectName"
          >We recommend using the domain name where you will deploy to as Project name.</label
        >
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
        <label for="projectPurpose" class="form-field__error-message">
          This field is required.
        </label>
      </UIFormField>
    </div>
    <div class="form-footer">
      <p>All fields marked with an asterisk (*) are required</p>
      <UIButton :leading-addon="SuitcaseTag" type="submit">
        Start your journey with Galley
      </UIButton>
    </div>
  </form>
</template>
<script setup lang="ts">
import UIFormField from '@/components/FormField/UIFormField.vue'
import UILabel from '@/components/FormField/UILabel.vue'
import { SuitcaseTag } from '@solar-icons/vue'
import { computed, reactive, ref, toRefs } from 'vue'
import UIButton from '@/components/UIButton.vue'
import UITextInput from '@/components/FormField/UITextInput.vue'
import SlashesDivider from '@/assets/SlashesDivider.vue'
import UIDropDown from '@/components/FormField/UIDropDown.vue'
import { useOnboardingStore } from '@/stores/onboarding.ts'
import { storeToRefs } from 'pinia'
import router from '@/router'

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

const formRef = ref<HTMLFormElement | null>(null)

const onboardingStore = useOnboardingStore()

const { charter, project } = storeToRefs(onboardingStore)

function onSubmit() {
  const form = formRef.value!

  form.reportValidity()
  if (!form.checkValidity()) {
    form.reportValidity() // shows browser messages
    return
  }

  onboardingStore.$patch({
    charter: charter.value,
    project: project.value,
    completed: {
      firstCharter: true,
    },
  })

  router.push('/onboarding/boarding')
}
</script>
