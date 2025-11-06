<template>
  <div class="absolute inset-0">
    <UIConfetti v-if="isCharterCreateSuccess && isProjectCreateSuccess" autoplay />
  </div>
  <div class="relative flex flex-col gap-8 max-w-4xl">
    <div class="space-y-2">
      <h1>Boarding the Vessel</h1>
      <p class="text-tides-900">
        This is the moment we will launch your Vessel into Galley. We’ll set up your account, your
        business profile vessel, first charter & project. This may take a minute. Once we’re ready
        we welcome you onboard into Galley.
      </p>
    </div>

    <div
      class="flex gap-2.5"
      :class="!isSignUpPending && !isSignUpSuccess && !isSignUpError && 'opacity-50'"
    >
      <Forward2 v-if="!isSignUpPending && !isSignUpSuccess && !isSignUpError" />
      <LoadingIndicator v-if="isSignUpPending" />
      <CheckCircle v-if="isSignUpSuccess" />
      <DangerCircle v-if="isSignUpError" />
      <div>
        <h4 class="text-navy-700">Launching your Vessel</h4>
        <p class="text-tides-900" v-if="!signUpError?.message">
          Watch out for the splash zone! We are now creating your cluster and your first namespaces.
        </p>
        <p v-else class="text-coral-500">
          {{ signUpError.message }}
        </p>
      </div>
    </div>

    <div
      class="flex gap-2.5"
      :class="
        !isVesselBillingProfilePending &&
        !isVesselBillingProfileSuccess &&
        !isVesselBillingProfileError &&
        'opacity-50'
      "
    >
      <Forward2
        v-if="
          !isVesselBillingProfilePending &&
          !isVesselBillingProfileSuccess &&
          !isVesselBillingProfileError
        "
      />
      <LoadingIndicator v-if="isVesselBillingProfilePending" />
      <CheckCircle v-if="isVesselBillingProfileSuccess" />
      <DangerCircle v-if="isVesselBillingProfileError" />
      <div>
        <h4 class="text-navy-700">Creating your account</h4>
        <p class="text-tides-900" v-if="!vesselBillingProfileError?.message">
          We’ll create your account and assign you the Captain of the Vessel
        </p>
        <p v-else class="text-coral-500">
          {{ vesselBillingProfileError.message }}
        </p>
      </div>
    </div>

    <div
      class="flex gap-2.5"
      :class="
        !isCharterCreatePending &&
        !isProjectCreatePending &&
        !isCharterCreateSuccess &&
        !isProjectCreateSuccess &&
        !isCharterCreateError &&
        !isProjectCreateError &&
        'opacity-50'
      "
    >
      <Forward2
        v-if="
          !isCharterCreatePending &&
          !isProjectCreatePending &&
          !isCharterCreateSuccess &&
          !isProjectCreateSuccess &&
          !isCharterCreateError &&
          !isProjectCreateError
        "
      />
      <LoadingIndicator v-if="isCharterCreatePending || isProjectCreatePending" />
      <CheckCircle v-if="isCharterCreateSuccess && isProjectCreateSuccess" />
      <DangerCircle v-if="isCharterCreateError || isProjectCreateError" />
      <div>
        <h4 class="text-navy-700">Embark with your first Charter and Projects</h4>
        <p
          class="text-tides-900"
          v-if="!projectCreateError?.message && !charterCreateError?.message"
        >
          We’ll add your first customer environment (Charter) and website (Projects) to set sail with
          Galley.
        </p>
        <p v-else-if="projectCreateError" class="text-coral-500">
          {{ projectCreateError.message }}
        </p>
        <p v-else-if="charterCreateError" class="text-coral-500">
          {{ charterCreateError.message }}
        </p>
      </div>
    </div>
    <div class="form-footer justify-end">
      <UIButton
        to="/"
        :aria-disabled="isCharterCreateSuccess && isProjectCreateSuccess"
        :leading-addon="ConfettiMinimalistic"
      >
        Step aboard Galley
      </UIButton>
    </div>
  </div>
</template>

<script setup lang="ts">
import { CheckCircle, ConfettiMinimalistic, DangerCircle, Forward2 } from '@solar-icons/vue'
import { onBeforeUnmount, onMounted } from 'vue'
import LoadingIndicator from '@/assets/LoadingIndicator.vue'
import UIButton from '@/components/UIButton.vue'
import UIConfetti from '@/components/UIConfetti.vue'
import { useMutation } from '@tanstack/vue-query'
import axios from 'axios'
import {
  type Charter,
  type Inquiry,
  type Intent,
  type Projects,
  useOnboardingStore,
  type User,
  type Vessel,
  type VesselBillingProfile,
} from '@/stores/onboarding.ts'
import { storeToRefs } from 'pinia'
import { useAuthStore } from '@/stores/auth.ts'
import router from '@/router'

const onboardingStore = useOnboardingStore()
const authStore = useAuthStore()

const { user, inquiry, intent, vessel, vesselBillingProfile, charter, project } =
  storeToRefs(onboardingStore)
const { activeVesselId } = storeToRefs(authStore)

const {
  isPending: isSignUpPending,
  isError: isSignUpError,
  error: signUpError,
  data: signUpData,
  isSuccess: isSignUpSuccess,
  mutateAsync: mutateSignUp,
} = useMutation({
  mutationFn: (signUp: { vessel: Vessel; user: User; inquiry: Inquiry; intent: Intent }) =>
    axios.post('/auth/sign-up', signUp),
})

const {
  isPending: isVesselBillingProfilePending,
  isError: isVesselBillingProfileError,
  error: vesselBillingProfileError,
  isSuccess: isVesselBillingProfileSuccess,
  mutateAsync: mutateVesselBillingProfile,
} = useMutation({
  mutationFn: (vesselBillingProfile: VesselBillingProfile) =>
    axios.post(`/vessels/${activeVesselId?.value}/billing-profile`, vesselBillingProfile),
})

const {
  isPending: isCharterCreatePending,
  isError: isCharterCreateError,
  error: charterCreateError,
  data: charterData,
  isSuccess: isCharterCreateSuccess,
  mutateAsync: mutateCharterCreate,
} = useMutation({
  mutationFn: (charter: Charter) =>
    axios.post(`/vessels/${activeVesselId?.value}/charters`, charter),
})

const {
  isPending: isProjectCreatePending,
  isError: isProjectCreateError,
  error: projectCreateError,
  isSuccess: isProjectCreateSuccess,
  mutateAsync: mutateProjectCreate,
} = useMutation({
  mutationFn: (project: Projects) =>
    axios.post(
      `/vessels/${activeVesselId?.value}/charters/${charterData.value?.data?.id}/projects`,
      project,
    ),
})

const timers: number[] = []

onMounted(async () => {
  await mutateSignUp({
    intent: intent.value,
    user: user.value,
    inquiry: inquiry.value,
    vessel: vessel.value,
  })
  await authStore.setRefreshToken(signUpData?.value?.data?.refreshToken)

  if (intent.value === 'business') {
    await mutateVesselBillingProfile(vesselBillingProfile.value)
  }

  if (isSignUpError.value || isVesselBillingProfileError.value) {
    return
  }

  await mutateCharterCreate(charter.value)

  if (isCharterCreateError.value) {
    return
  }

  await mutateProjectCreate(project.value)

  if (isProjectCreateError.value) {
    return
  }

  await router.push('/')
})

onBeforeUnmount(() => {
  timers.forEach(clearTimeout)
})
</script>
