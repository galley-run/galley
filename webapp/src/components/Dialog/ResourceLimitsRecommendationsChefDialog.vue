<template>
  <UIDialog class="dialog--chef" :show="show" @close="!showKubernetesTerms && $emit('close')">
    <div class="dialog__header">
      <InfoCircle />
      <h3>Resource Size &amp; Resource Limits</h3>
      <UIButton @click="$emit('close')" ghost variant="neutral" :trailing-addon="CloseCircle" />
    </div>
    <div class="dialog__body space-y-4">
      <h3>Resource size</h3>
      <p>Resource size defines the minimum amount of CPU and memory to run properly.</p>
      <p>
        It's what will be reserved for each application or database
        <abbr
          title="in Kubernetes terms"
          class="cursor-pointer"
          @click="showKubernetesTerms = KubernetesTerms.pods"
          >(Pod)</abbr
        >
        to run. If your node doesn't have the required resources it won't start running.
      </p>

      <h3>Resource limits</h3>
      <p>
        Resource limits however, define the maximum amount of CPU and memory your container is
        allowed to use. If your app exceeds this limit:
      </p>
      <ul>
        <li>
          If memory limit is hit → the container will be killed (<abbr
            class="cursor-pointer"
            title="Out of Memory Killed"
            @click="showKubernetesTerms = KubernetesTerms.oomkilled"
            >OOMKilled</abbr
          >)
        </li>
        <li>If CPU limit is hit → the container will be throttled</li>
      </ul>

      <h3>Example</h3>
      <p>
        Below you'll find 4 nodes (servers), each filled element represents an application with set
        resource size. An dashed extension to such element represents the resource limits higher
        than the resource size.
      </p>
      <div class="grid gap-4 grid-cols-[1fr_1fr_1fr_1fr] mt-8">
        <div class="group">
          <div class="rounded-lg bg-navy-100 p-4 h-48 flex flex-col justify-end">
            <div class="rounded-lg outline-0 transition-all duration-300 group-hover:outline-2 outline-offset-2 outline-green-800 outline-dotted  bg-tides-800 h-14 mb-4"></div>
            <div class="rounded-lg outline-0 transition-all duration-300 group-hover:outline-2 outline-offset-2 outline-green-800 outline-dotted  bg-tides-800 h-14"></div>
          </div>
          <p class="text-sm mt-2 text-tides-800">Will deploy, perfect distribution</p>
        </div>
        <div class="group">
          <div class="rounded-lg bg-navy-100 p-4 h-48 flex flex-col justify-end">
            <div
              class="rounded-t-lg border-4 border-b-0 border-tides-800 border-dashed h-6 w-full"
            ></div>
            <div class="rounded-lg bg-tides-800 h-12 mb-4 outline-0 transition-all duration-300 group-hover:outline-2 outline-offset-2 outline-green-800 outline-dotted"></div>
            <div
              class="rounded-t-lg border-4 border-b-0 border-tides-800 border-dashed h-6 w-full"
            ></div>
            <div class="rounded-lg bg-tides-800 h-12 outline-0 transition-all duration-300 group-hover:outline-2 outline-offset-2 outline-green-800 outline-dotted "></div>
          </div>
          <p class="text-sm mt-2 text-tides-800">
            Will deploy, perfect in normal conditions, tight in bursts.
          </p>
        </div>
        <div class="group">
          <div class="rounded-lg bg-navy-100 p-4 h-48">
            <div
              class="rounded-t-lg border-4 border-b-0 border-tides-800 border-dashed h-6 w-full -mt-6"
            ></div>
            <div class="rounded-lg bg-tides-800 h-18 outline-0 transition-all duration-300 group-hover:outline-2 outline-offset-2 outline-cliona-600 outline-dotted  mb-4"></div>
            <div class="rounded-lg bg-tides-800 h-18 outline-0 transition-all duration-300 group-hover:outline-2 outline-offset-2 outline-cliona-600 outline-dotted "></div>
          </div>
          <p class="text-sm mt-2 text-tides-800">
            Will deploy, tight fit in normal conditions, issues may show when under pressure.
          </p>
        </div>
        <div class="group">
          <div class="rounded-lg bg-navy-100 p-4 max-h-48">
            <div class="rounded-lg bg-tides-800 outline-0 transition-all duration-300 group-hover:outline-2 outline-offset-2 outline-red-800 outline-dotted h-14 mb-4 -mt-10"></div>
            <div class="rounded-lg bg-tides-800 outline-0 transition-all duration-300 group-hover:outline-2 outline-offset-2 outline-green-800 outline-dotted h-14 mb-4"></div>
            <div class="rounded-lg bg-tides-800 outline-0 transition-all duration-300 group-hover:outline-2 outline-offset-2 outline-green-800 outline-dotted h-14"></div>
          </div>
          <p class="text-sm mt-2 text-tides-800">Only 2 of 3 applications will deploy</p>
        </div>
      </div>

      <div class="alert alert--info my-8">
        <div class="alert__body">
          <h6>Default Resource Limits</h6>
          <p>
            By default Burstable instance is disabled in the Galley UI. When disabled we will
            automatically set your resource size as resource limits.
          </p>
          <p>
            This way your applications will by default never use more computing power than there is
            available on your node.
          </p>
        </div>
      </div>

      <h3>In conclusion</h3>
      <p>
        <strong>Requests Size</strong> is what your app is guaranteed<br />
        <strong>Request Limits</strong> is what your app is allowed to peak at
      </p>
    </div>
  </UIDialog>
  <KubernetesTermsRecommendationsChefDialog
    :stacked="1"
    :show="!!showKubernetesTerms"
    :kubernetes-term="showKubernetesTerms"
    @close="() => (showKubernetesTerms = undefined)"
  />
</template>
<script setup lang="ts">
import { CloseCircle, InfoCircle } from '@solar-icons/vue'
import UIButton from '@/components/UIButton.vue'
import UIDialog from '@/components/Dialog/UIDialog.vue'
import { ref } from 'vue'
import KubernetesTermsRecommendationsChefDialog from '@/components/Dialog/KubernetesTermsRecommendationsChefDialog.vue'
import { KubernetesTerms } from '@/utils/kubernetes.ts'

const { show } = defineProps<{ show: boolean }>()
defineEmits<{ (e: 'close'): void }>()

const showKubernetesTerms = ref<KubernetesTerms | undefined>(undefined)
</script>
