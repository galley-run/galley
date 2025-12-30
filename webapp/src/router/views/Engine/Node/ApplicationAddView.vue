<template>
  <div class="flex flex-col gap-8">
    <div class="card">
      <h1>Deploy a new application</h1>
      <p class="mb-6">
        No matter if you have a repo you want to deploy from or already push your Docker container
        to a private or public registry. Galley got you covered.
      </p>

      <ol class="stepper">
        <li
          v-for="i in steps"
          v-bind:key="i"
          :aria-checked="i < currentStep"
          :aria-current="i === currentStep ? 'step' : undefined"
        >
          <span v-if="i < currentStep">
            <CheckCircle />
          </span>
          <span v-else>
            {{ i }}
          </span>
        </li>
      </ol>
    </div>

    <form ref="formRef" novalidate @submit.prevent="onSubmit">
      <div class="card mx-auto max-w-4xl" v-if="currentStep === 1">
        <h2>Choose Deployment Method</h2>
        <p class="mb-6">Select how you want to deploy your application</p>

        <UIFormField>
          <div class="grid grid-cols-2 gap-4">
            <UIRadioCard
              title="Git Repository"
              description="Deploy from a Git Repository hosted on GitHub, GitLab or Bitbucket."
              name="deploymentMethod"
              required
              value="git"
              v-model="deploymentMethod"
            >
              <template v-slot:icon>
                <IconBranch class="text-navy-600" />
              </template>
            </UIRadioCard>
            <UIRadioCard
              title="Docker Registry"
              description="Deploy from a Docker container image in a Docker registry, such as Docker Hub or GitHub Container Registry."
              name="deploymentMethod"
              required
              value="docker"
              v-model="deploymentMethod"
            >
              <template v-slot:icon>
                <Box class="text-navy-600" />
              </template>
            </UIRadioCard>
          </div>
          <label for="deploymentMethod" class="form-field__error-message">
            Please choose a deployment method before you continue.
          </label>
        </UIFormField>

        <div class="card__footer form-footer">
          <UIButton ghost variant="neutral" :leading-addon="ArrowLeft" to="/application">
            Back
          </UIButton>
          <UIButton type="submit" :trailing-addon="ArrowRight">Continue</UIButton>
        </div>
      </div>

      <div class="card mx-auto max-w-4xl" v-if="currentStep === 2 && deploymentMethod === 'git'">
        <h2>Connect to a Git Provider</h2>
        <p class="mb-6">Authenticate with your Git provider to access repositories</p>

        <div v-if="isLoadingConnections" class="mb-6 text-center text-gray-500">
          Loading existing connections...
        </div>

        <UIFormField>
          <div class="grid grid-cols-1 gap-4">
            <UIRadioCard
              title="GitHub"
              :description="
                getConnectionForProvider('github', 'git')
                  ? '✓ Connected to your GitHub account.'
                  : 'Connect to your GitHub account.'
              "
              name="gitProvider"
              required
              value="github"
              v-model="gitProvider"
            >
              <template v-slot:icon>
                <IconGitHub class="text-navy-600" />
              </template>
            </UIRadioCard>
            <UIRadioCard
              title="GitLab"
              :description="
                getConnectionForProvider('gitlab', 'git')
                  ? '✓ Connected to your GitLab account.'
                  : 'Connect to your GitLab account.'
              "
              name="gitProvider"
              required
              value="gitlab"
              v-model="gitProvider"
            >
              <template v-slot:icon>
                <IconGitLab class="text-navy-600" />
              </template>
            </UIRadioCard>
            <UIRadioCard
              title="BitBucket"
              :description="
                getConnectionForProvider('bitbucket', 'git')
                  ? '✓ Connected to your Bitbucket account.'
                  : 'Connect to your Bitbucket account.'
              "
              name="gitProvider"
              required
              value="bitbucket"
              v-model="gitProvider"
            >
              <template v-slot:icon>
                <IconBitbucket class="text-navy-600" />
              </template>
            </UIRadioCard>
          </div>
          <label for="deploymentMethod" class="form-field__error-message">
            Please connect to a git provider before you continue.
          </label>
        </UIFormField>

        <div class="card__footer form-footer">
          <UIButton ghost variant="neutral" :leading-addon="ArrowLeft" @click="currentStep--">
            Back
          </UIButton>
          <UIButton type="submit" :trailing-addon="ArrowRight" :disabled="isStartingOAuth">
            {{ isStartingOAuth ? 'Connecting...' : 'Continue' }}
          </UIButton>
        </div>
      </div>

      <div class="card mx-auto max-w-4xl" v-if="currentStep === 2 && deploymentMethod === 'docker'">
        <h2>Connect to a Docker Registry</h2>
        <p class="mb-6">Authenticate with your Docker registry to access your Docker images</p>

        <div v-if="isLoadingConnections" class="mb-6 text-center text-gray-500">
          Loading existing connections...
        </div>

        <UIFormField>
          <div class="grid grid-cols-2 gap-4">
            <UIRadioCard
              title="GitHub"
              :description="
                getConnectionForProvider('github', 'registry')
                  ? '✓ Connected - Deploy from ghcr.io registry.'
                  : 'Deploy from ghcr.io registry.'
              "
              name="dockerProvider"
              required
              value="github"
              v-model="dockerProvider"
            >
              <template v-slot:icon>
                <IconBranch class="text-navy-600" />
              </template>
            </UIRadioCard>
            <UIRadioCard
              title="Docker Hub"
              description="Deploy from public Docker Hub images."
              name="dockerProvider"
              required
              value="dockerhub"
              v-model="dockerProvider"
            >
              <template v-slot:icon>
                <Box class="text-navy-600" />
              </template>
            </UIRadioCard>
          </div>
          <label for="deploymentMethod" class="form-field__error-message">
            Please connect to a docker registry before you continue.
          </label>
        </UIFormField>

        <div class="card__footer form-footer">
          <UIButton ghost variant="neutral" :leading-addon="ArrowLeft" @click="currentStep--">
            Back
          </UIButton>
          <UIButton type="submit" :trailing-addon="ArrowRight" :disabled="isStartingOAuth">
            {{ isStartingOAuth ? 'Connecting...' : 'Continue' }}
          </UIButton>
        </div>
      </div>

      <div class="card mx-auto max-w-4xl" v-if="currentStep === 3 && deploymentMethod === 'git'">
        <h2>Select Repository</h2>
        <p class="mb-6">Choose a repository from your connected {{ gitProvider }} account</p>

        <div v-if="isLoadingRepositories" class="mb-6 text-center text-gray-500">
          Loading repositories...
        </div>

        <div v-else-if="repositories.length === 0" class="mb-6 text-center text-gray-500">
          No repositories found. Make sure your connection has access to repositories.
        </div>

        <div v-else>
          <div class="grid grid-cols-3 gap-4 mb-4">
            <UIFormField v-if="uniqueAccounts.length > 1">
              <label class="form-field__label">Account</label>
              <UIDropDown
                v-model="selectedAccount"
                :items="accountDropdownItems"
                placeholder="All accounts"
                @change="filterRepositoriesByAccount"
              />
            </UIFormField>

            <UIFormField class="col-span-2">
              <label class="form-field__label">Repository</label>
              <UIAutoComplete
                v-model="repositorySearch"
                :items="repositoryAutocompleteItems"
                placeholder="Search repositories..."
                auto-open
                :icon="IconBranch"
              />
              <label for="repository" class="form-field__error-message">
                Please select a repository before you continue.
              </label>
            </UIFormField>
          </div>

          <UIRadioCard
            v-if="selectedRepositoryObject"
            :title="selectedRepositoryObject.full_name"
            :description="selectedRepositoryObject.description || 'No description available'"
            name="selectedRepository"
            :value="selectedRepositoryObject.id"
            :model-value="selectedRepositoryObject.id"
            checked
          >
            <template v-slot:icon>
              <IconBranch class="text-navy-600" />
            </template>
          </UIRadioCard>
        </div>

        <div class="card__footer form-footer">
          <UIButton ghost variant="neutral" :leading-addon="ArrowLeft" @click="currentStep--">
            Back
          </UIButton>
          <UIButton type="submit" :trailing-addon="ArrowRight" :disabled="!selectedRepository">
            Continue
          </UIButton>
        </div>
      </div>

      <div class="card mx-auto max-w-4xl" v-if="currentStep === 3 && deploymentMethod === 'docker'">
        <h2>Container Image</h2>
        <p class="mb-6">Specify the container image and tag to deploy</p>

        <div class="grid grid-cols-3 gap-4">
          <UIFormField class="col-span-2">
            <label class="form-field__label" for="containerImage">Container Image</label>
            <UITextInput
              id="containerImage"
              name="containerImage"
              v-model="containerImage"
              :placeholder="
                dockerProvider === 'github' ? 'e.g. ghcr.io/owner/image' : 'e.g. nginx, myorg/myapp'
              "
              required
            />
            <label for="containerImage" class="form-field__error-message">
              Please enter a container image name.
            </label>
          </UIFormField>

          <UIFormField>
            <label class="form-field__label" for="containerTag">Container Tag</label>
            <UITextInput
              id="containerTag"
              name="containerTag"
              v-model="containerTag"
              placeholder="latest"
              required
            />
            <label for="containerTag" class="form-field__error-message">
              Please enter a container tag.
            </label>
          </UIFormField>
        </div>

        <div class="card__footer form-footer">
          <UIButton ghost variant="neutral" :leading-addon="ArrowLeft" @click="currentStep--">
            Back
          </UIButton>
          <UIButton type="submit" :trailing-addon="ArrowRight"> Continue </UIButton>
        </div>
      </div>

      <div class="card mx-auto max-w-4xl" v-if="currentStep === 4 && deploymentMethod === 'git'">
        <h2>Configure Branch</h2>
        <p class="mb-6">Specify the branch to deploy from</p>

        <UIFormField>
          <label class="form-field__label" for="branch">Branch</label>
          <UITextInput id="branch" name="branch" v-model="branch" placeholder="main" required />
          <label for="branch" class="form-field__error-message">
            Please enter a branch name.
          </label>
        </UIFormField>

        <div class="card__footer form-footer">
          <UIButton ghost variant="neutral" :leading-addon="ArrowLeft" @click="currentStep--">
            Back
          </UIButton>
          <UIButton type="submit" :trailing-addon="ArrowRight"> Continue </UIButton>
        </div>
      </div>
    </form>
  </div>
</template>
<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft, ArrowRight, Box, CheckCircle } from '@solar-icons/vue'
import UIRadioCard from '@/components/FormField/UIRadioCard.vue'
import UIButton from '@/components/UIButton.vue'
import IconBranch from '@/components/CustomIcon/IconBranch.vue'
import UIFormField from '@/components/FormField/UIFormField.vue'
import UIAutoComplete from '@/components/FormField/UIAutoComplete.vue'
import UIDropDown from '@/components/FormField/UIDropDown.vue'
import UITextInput from '@/components/FormField/UITextInput.vue'
import IconGitHub from '@/components/CustomIcon/IconGitHub.vue'
import IconBitbucket from '@/components/CustomIcon/IconBitbucket.vue'
import IconGitLab from '@/components/CustomIcon/IconGitLab.vue'
import { useCharterId, useVesselId } from '@/composables/useResourceHelpers'
import type { OAuthConnectionType, OAuthProvider } from '@/types/oauth'
import axios from 'axios'

const route = useRoute()
const vesselId = useVesselId()
const charterId = useCharterId()

const currentStep = ref(1)

// Compute total steps based on deployment method
const steps = ref(4)

// Check if we're returning from OAuth callback
onMounted(() => {
  const step = route.query.step
  if (step && typeof step === 'string') {
    const stepNumber = parseInt(step, 10)
    if (!isNaN(stepNumber) && stepNumber > 0 && stepNumber <= steps.value) {
      currentStep.value = stepNumber
    }
  }

  // Fetch connections if we're already on step 2
  if (currentStep.value === 2) {
    fetchExistingConnections()
  }
})

const deploymentMethod = ref('')
const gitProvider = ref('')
const dockerProvider = ref('')
const selectedRepository = ref('')
const repositorySearch = ref('')
const selectedAccount = ref('')
const filteredRepositories = ref<any[]>([])
const branch = ref('main')
const containerImage = ref('')
const containerTag = ref('latest')

const formRef = ref<HTMLFormElement | null>(null)
const isStartingOAuth = ref(false)
const existingConnections = ref<any[]>([])
const isLoadingConnections = ref(false)
const repositories = ref<any[]>([])
const isLoadingRepositories = ref(false)
const activeConnectionId = ref<string | null>(null)

async function fetchExistingConnections() {
  if (!vesselId.value || !charterId.value) return

  isLoadingConnections.value = true
  try {
    const response = await axios.get(
      `/vessels/${vesselId.value}/charters/${charterId.value}/connections`,
    )
    console.log('Fetched connections:', response)
    // Filter for active connections only
    existingConnections.value = (response || []).filter((conn: any) => conn.status === 'active')
    console.log('Active connections:', existingConnections.value)
  } catch (error) {
    console.error('Failed to fetch existing connections:', error)
    existingConnections.value = []
  } finally {
    isLoadingConnections.value = false
  }
}

function getConnectionForProvider(provider: OAuthProvider, type: OAuthConnectionType) {
  const connection = existingConnections.value.find(
    (conn: any) => conn.provider === provider && conn.type === type && conn.status === 'active',
  )
  console.log(`Looking for connection: provider=${provider}, type=${type}, found=`, connection)
  return connection
}

async function fetchRepositories() {
  if (!vesselId.value || !charterId.value) return

  // Get the active connection based on deployment method
  let provider: OAuthProvider
  let type: OAuthConnectionType

  if (deploymentMethod.value === 'git') {
    provider = gitProvider.value as OAuthProvider
    type = 'git'
  } else if (deploymentMethod.value === 'docker' && dockerProvider.value === 'github') {
    provider = 'github'
    type = 'registry'
  } else {
    return
  }

  const connection = getConnectionForProvider(provider, type)

  if (!connection) {
    console.error('No active connection found')
    return
  }

  activeConnectionId.value = connection.id
  isLoadingRepositories.value = true

  try {
    const response = await axios.get(
      `/vessels/${vesselId.value}/charters/${charterId.value}/connections/${connection.id}/repositories`,
    )
    console.log('Fetched repositories:', response)
    // Axios interceptor unwraps response.data.data ?? response.data
    // If API returns { data: [...], meta: {...} }, response will be the data array
    repositories.value = Array.isArray(response) ? response : response?.data || []
    filteredRepositories.value = repositories.value
  } catch (error) {
    console.error('Failed to fetch repositories:', error)
    repositories.value = []
    filteredRepositories.value = []
  } finally {
    isLoadingRepositories.value = false
  }
}

// Extract unique accounts from repositories
const uniqueAccounts = computed(() => {
  const accounts = new Set<string>()
  repositories.value.forEach((repo) => {
    // Extract account/owner from full_name (e.g., "owner/repo-name")
    const owner = repo.full_name?.split('/')[0]
    if (owner) accounts.add(owner)
  })
  return Array.from(accounts).sort()
})

// Format accounts for dropdown
const accountDropdownItems = computed(() => {
  return [
    { label: 'All accounts', value: '' },
    ...uniqueAccounts.value.map((account) => ({
      label: account,
      value: account,
    })),
  ]
})

// Format repositories for autocomplete
const repositoryAutocompleteItems = computed(() => {
  return filteredRepositories.value.map((repo) => ({
    label: repo.full_name,
    value: repo.id,
  }))
})

// Get the selected repository object
const selectedRepositoryObject = computed(() => {
  return repositories.value.find((repo) => repo.id === repositorySearch.value)
})

// Watch for changes to selectedRepositoryObject to update selectedRepository
watch(repositorySearch, (value) => {
  selectedRepository.value = value
})

// Fetch existing connections when moving to step 2
watch(currentStep, (newStep) => {
  if (newStep === 2) {
    fetchExistingConnections()
  } else if (newStep === 3 && deploymentMethod.value === 'git') {
    fetchRepositories()
  }
})

// Auto-continue when deployment method is selected
watch(deploymentMethod, (newValue) => {
  if (newValue && currentStep.value === 1) {
    currentStep.value = 2
  }
})

// Track if we should auto-continue after connections load
const shouldAutoContinue = ref(false)

// Auto-continue when git provider is selected
watch(gitProvider, (newValue) => {
  if (newValue && currentStep.value === 2 && deploymentMethod.value === 'git') {
    if (isLoadingConnections.value) {
      // Still loading, flag to continue after load
      shouldAutoContinue.value = true
    } else {
      // Already loaded, continue immediately
      onSubmit()
    }
  }
})

// Auto-continue when docker provider is selected
watch(dockerProvider, (newValue) => {
  if (newValue && currentStep.value === 2 && deploymentMethod.value === 'docker') {
    if (isLoadingConnections.value) {
      // Still loading, flag to continue after load
      shouldAutoContinue.value = true
    } else {
      // Already loaded, continue immediately
      onSubmit()
    }
  }
})

// Auto-continue after connections finish loading (if flagged)
watch(isLoadingConnections, (newValue, oldValue) => {
  if (oldValue && !newValue && shouldAutoContinue.value) {
    shouldAutoContinue.value = false
    onSubmit()
  }
})

function filterRepositoriesByAccount() {
  if (selectedAccount.value === '') {
    filteredRepositories.value = repositories.value
  } else {
    filteredRepositories.value = repositories.value.filter((repo) =>
      repo.full_name?.startsWith(selectedAccount.value + '/'),
    )
  }
  // Reset search when account changes
  repositorySearch.value = ''
}

function openOAuthPopup(provider: string): {
  popup: Window | null
  promise: Promise<void>
  navigate: (url: string) => void
} {
  const width = 600
  const height = 700
  const left = window.screenX + (window.outerWidth - width) / 2
  const top = window.screenY + (window.outerHeight - height) / 2

  // Open popup immediately with loading page
  const popup = window.open(
    'about:blank',
    `oauth-${provider}`,
    `width=${width},height=${height},left=${left},top=${top},toolbar=no,location=no,status=no,menubar=no,scrollbars=yes,resizable=yes`,
  )

  let checkClosedInterval: number | null = null

  const promise = new Promise<void>((resolve, reject) => {
    if (!popup) {
      reject(new Error('Popup blocked'))
      return
    }

    // Show loading message
    try {
      popup.document.write(
        '<html><body style="display:flex;align-items:center;justify-content:center;height:100vh;font-family:sans-serif;"><div>Loading OAuth...</div></body></html>',
      )
      popup.document.close()
    } catch (e) {
      console.error('Failed to write to popup:', e)
    }

    // Listen for messages from the callback page
    const messageHandler = async (event: MessageEvent) => {
      // Verify origin for security
      if (event.origin !== window.location.origin) return

      if (event.data.type === 'oauth-callback') {
        console.log('OAuth callback received with params:', event.data.params)
        window.removeEventListener('message', messageHandler)
        if (checkClosedInterval) clearInterval(checkClosedInterval)
        if (popup && !popup.closed) popup.close()

        // Now make the approval call in the main window (with auth token)
        try {
          const response = await axios.post(
            `/vessels/${vesselId.value}/charters/${charterId.value}/connections/approval`,
            {
              provider: event.data.provider,
              ...event.data.params,
            },
          )
          console.log('Approval successful:', response)
          resolve()
        } catch (error) {
          console.error('Approval failed:', error)
          reject(error)
        }
      } else if (event.data.type === 'oauth-success') {
        console.log('OAuth success received')
        window.removeEventListener('message', messageHandler)
        if (checkClosedInterval) clearInterval(checkClosedInterval)
        if (popup && !popup.closed) popup.close()
        resolve()
      } else if (event.data.type === 'oauth-error') {
        console.log('OAuth error received:', event.data.error)
        window.removeEventListener('message', messageHandler)
        if (checkClosedInterval) clearInterval(checkClosedInterval)
        if (popup && !popup.closed) popup.close()
        reject(new Error(event.data.error || 'OAuth failed'))
      }
    }

    window.addEventListener('message', messageHandler)

    // Check if popup was closed manually - but only start after navigation
    const startCheckingClosed = () => {
      checkClosedInterval = setInterval(() => {
        if (popup.closed) {
          clearInterval(checkClosedInterval!)
          window.removeEventListener('message', messageHandler)
          reject(new Error('Popup closed'))
        }
      }, 500)
    }

    // Don't start checking immediately
    setTimeout(startCheckingClosed, 1000)
  })

  const navigate = (url: string) => {
    if (popup && !popup.closed) {
      console.log('Navigating popup to:', url)
      try {
        popup.location.href = url
        console.log('Navigation initiated')
      } catch (e) {
        console.error('Failed to navigate popup:', e)
        reject(new Error('Failed to navigate to OAuth provider'))
      }
    } else {
      console.error('Popup is closed or null')
      reject(new Error('Popup closed before navigation'))
    }
  }

  return { popup, promise, navigate }
}

async function onSubmit() {
  const form = formRef.value!

  form.reportValidity()
  if (!form.checkValidity()) {
    form.reportValidity() // shows browser messages
    return
  }

  // If we're on step 2 and a provider is selected, start OAuth flow
  if (currentStep.value === 2) {
    // Docker Hub doesn't require OAuth - skip directly to step 3
    if (deploymentMethod.value === 'docker' && dockerProvider.value === 'dockerhub') {
      currentStep.value = 3
      return
    }

    let provider: string | null = null
    let type: OAuthConnectionType | null = null

    if (deploymentMethod.value === 'git' && gitProvider.value) {
      provider = gitProvider.value
      type = 'git'
    } else if (deploymentMethod.value === 'docker' && dockerProvider.value === 'github') {
      provider = 'github'
      type = 'registry'
    }

    if (provider && type) {
      let popup: Window | null = null
      try {
        isStartingOAuth.value = true

        // Check if there's already an active connection for this provider/type
        const existingConnection = getConnectionForProvider(provider as OAuthProvider, type)

        if (existingConnection) {
          // Connection exists and is active - skip OAuth and go to next step
          console.log('Using existing connection:', existingConnection)
          activeConnectionId.value = existingConnection.id
          currentStep.value = 3
          isStartingOAuth.value = false
          return
        }

        // Open popup immediately (before async call) to avoid popup blocker
        const popupResult = openOAuthPopup(provider)
        popup = popupResult.popup

        if (!popup) {
          alert('Popup was blocked. Please allow popups for this site.')
          return
        }

        // Determine required scopes based on provider and type
        let scopes: string[] = []
        if (provider === 'github') {
          if (type === 'git') {
            scopes = ['repo']
          } else if (type === 'registry') {
            scopes = ['read:packages']
          }
        } else if (provider === 'gitlab') {
          scopes = ['read_api', 'read_repository']
        } else if (provider === 'bitbucket') {
          scopes = ['repository']
        }

        // Create OAuth connection and get authorization URL
        const response = await axios.post(
          `/vessels/${vesselId.value}/charters/${charterId.value}/connections`,
          {
            type,
            provider,
            scopes,
          },
        )

        console.log('OAuth connection response:', response)

        // Axios interceptor already unwraps response.data.data or response.data
        const authorizationUrl = response?.attributes?.authorization_url

        console.log('Authorization URL:', authorizationUrl)

        if (authorizationUrl) {
          console.log('About to navigate popup...')
          // Navigate the already-open popup to the OAuth URL
          popupResult.navigate(authorizationUrl)

          console.log('Waiting for OAuth to complete...')
          // Wait for OAuth to complete
          await popupResult.promise

          console.log('OAuth completed successfully!')
          // Refresh the connections list to show the new connection
          await fetchExistingConnections()
          // On success, move to next step
          currentStep.value = 3
        } else {
          console.error('No authorization URL received')
          console.error('Full response:', JSON.stringify(response, null, 2))
          popup.close()
        }
      } catch (error) {
        console.error('Failed to complete OAuth flow:', error)
        if (popup && !popup.closed) popup.close()
        alert('Failed to connect. Please try again.')
      } finally {
        isStartingOAuth.value = false
      }
      return
    }
  }

  currentStep.value += 1
}
</script>
