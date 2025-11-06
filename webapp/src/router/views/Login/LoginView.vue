<template>
  <form @submit.prevent="onSubmit" ref="formRef" class="space-y-8 max-w-4xl" novalidate>
    <div class="space-y-2">
      <h1>Greetings!</h1>
      <p class="text-tides-900">Please sign in to continue.</p>
    </div>

    <UIFormField>
      <UILabel for="email">Email address</UILabel>
      <UITextInput
        id="email"
        type="email"
        placeholder="e.g. captainjack@blackpearl.ship"
        v-model="email"
        required
      />
      <label for="email" class="form-field__error-message"
        >Your email adddress is a required field to sign in</label
      >
      <label for="email" class="form-field__error-message" v-if="isError">{{ error }}</label>
      <p class="text-coral-500 form-field-label" v-if="suggestSignUp">There doesn't seem to be an account for you on this email address.<br /><RouterLink to="/onboarding">Do you want to sign up instead?</RouterLink></p>
    </UIFormField>

    <div class="form-footer">
      <UIButton :disabled="isPending" type="submit"
        >Sign in <LoadingIndicator v-if="isPending"
      /></UIButton>
    </div>
  </form>


    <div class="border-t border-tides-400 pt-4 mt-4">
<!--      <p class="text-tides-800">Sign up instead</p>-->
      <UIButton ghost type="submit" to="/onboarding">Sign up instead</UIButton>
  <!--    <UIButton ghost type="submit">Sign in with Apple</UIButton>-->
    </div>
</template>
<script setup lang="ts">
import UIFormField from '@/components/FormField/UIFormField.vue'
import UILabel from '@/components/FormField/UILabel.vue'
import UITextInput from '@/components/FormField/UITextInput.vue'
import { ref } from 'vue'
import UIButton from '@/components/UIButton.vue'
import router from '@/router'
import { useMutation } from '@tanstack/vue-query'
import axios from 'axios'
import LoadingIndicator from '@/assets/LoadingIndicator.vue'
import { ApiError } from '@/utils/registerAxios.ts'
import { useAuthStore } from '@/stores/auth.ts'

const email = ref('')
const suggestSignUp = ref(false)
const formRef = ref<HTMLFormElement | null>(null)

const authStore = useAuthStore()

const { isPending, isError, error, isSuccess, mutateAsync } = useMutation({
  mutationFn: (email: string) => axios.post(`/auth/sign-in`, { email }),
})

async function onSubmit() {
  const form = formRef.value!
  suggestSignUp.value = false

  form.reportValidity()
  if (!form.checkValidity()) {
    form.reportValidity() // shows browser messages
    return
  }

  try {
    const result = await mutateAsync(email.value)

    if (isSuccess.value) {
      await authStore.setRefreshToken(result.data.data.refreshToken)
      await router.push('/')
    }
  } catch (error) {
    console.log(error)
    if (error instanceof ApiError) {
      if (error.response?.status === 404) {
        suggestSignUp.value = true
      }
    }
    console.error(error)
  }
}
</script>
