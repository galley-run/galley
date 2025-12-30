<template>
  <aside class="main-navigation">
    <div class="main-navigation__sections">
      <section v-for="(section, sIdx) in projectSections" :key="sIdx">
        <nav>
          <h6 class="text-tides-800 px-2 pb-2">{{ section.label }}</h6>
          <ul class="flex flex-col gap-0.5">
            <li v-for="(item, idx) in section.items" :key="idx">
              <RouterLink :to="item.href" exact-active-class="bg-navy-50" :tabindex="1" :aria-disabled="item.disabled">
                <component :is="item.icon" size="20" />
                {{ item.label }}
              </RouterLink>
            </li>
          </ul>
        </nav>
      </section>
      <SlashesDivider class="opacity-30 w-full px-2" />
      <section v-for="(section, sIdx) in vesselSections" :key="sIdx">
        <nav>
          <h6 class="text-tides-800 px-2 pb-2">{{ section.label }}</h6>
          <ul class="flex flex-col gap-0.5">
            <li v-for="(item, idx) in section.items" :key="idx">
              <RouterLink :to="item.href" exact-active-class="bg-navy-50" :tabindex="1" :aria-disabled="item.disabled">
                <component :is="item.icon" size="20" />
                {{ item.label }}
              </RouterLink>
            </li>
          </ul>
        </nav>
      </section>
    </div>
    <div
      class="flex items-center gap-2 text-sm p-0 text-navy-700 border-t border-tides-300 px-4 py-4"
    >
      <div v-if="scopes.indexOf('enterprise') > -1">Enterprise Edition</div>
      <div v-else>Community Edition</div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { useLicenseStore } from '@/stores/license.ts'

import {
  ClipboardList,
  CloudUpload,
  CodeFile,
  Database,
  Diploma,
  FolderWithFiles,
  Graph,
  LinkCircle,
  Password,
  PresentationGraph,
  Routing3,
  Server2,
  ServerPath,
  ServerSquareCloud,
  Settings,
  UsersGroupTwoRounded,
  WalletMoney,
} from '@solar-icons/vue'
import SlashesDivider from '@/assets/SlashesDivider.vue'

import { storeToRefs } from 'pinia'

const licenseStore = useLicenseStore()
const { scopes } = storeToRefs(licenseStore)

const projectSections = [
  {
    label: 'The Galley',
    items: [
      { icon: PresentationGraph, label: 'Dashboard', href: '/' },
      { icon: Routing3, label: 'Gateway', href: '/gateway', disabled: true, },
      { icon: Server2, label: 'Applications', href: '/application', },
      { icon: Database, label: 'Databases', href: '/databases', disabled: true, },
      { icon: FolderWithFiles, label: 'Object Storage', href: '/object-storage', disabled: true, },
    ],
  },
  {
    label: 'The Pantry',
    items: [
      { icon: Password, label: 'Secrets', href: '/secrets', disabled: true, },
      { icon: ClipboardList, label: 'Configs', href: '/configs', disabled: true, },
    ],
  },
  {
    label: "Captain's Quarters",
    items: [
      { icon: CloudUpload, label: 'Releases', href: '/releases', disabled: true, },
      { icon: Graph, label: 'Monitoring', href: '/monitoring', disabled: true, },
    ],
  },
  {
    label: 'Below Deck',
    items: [
      { icon: LinkCircle, label: 'Networking', href: '/networking', disabled: true, },
      { icon: CodeFile, label: 'Automations', href: '/automations', disabled: true, },
      { icon: Settings, label: 'Settings', href: '/settings', disabled: true, },
    ],
  },
]

const vesselSections = [
  {
    label: 'Charter',
    items: [
      { icon: Settings, label: 'Settings', href: '/charter/settings', disabled: true, },
      { icon: WalletMoney, label: 'Billing', href: '/charter/billing', disabled: true, },
      { icon: ServerSquareCloud, label: 'Compute Plans', href: '/charter/compute-plan' },
      { icon: Diploma, label: 'Compliance', href: '/charter/compliance', disabled: true, },
      { icon: UsersGroupTwoRounded, label: 'Crew', href: '/charter/crew', disabled: true, },
    ],
  },
  {
    label: 'Vessel',
    items: [
      { icon: Settings, label: 'Settings', href: '/vessel/settings', disabled: true, },
      { icon: WalletMoney, label: 'Billing', href: '/vessel/billing', disabled: true, },
      { icon: ServerPath, label: 'Engine', href: `/vessel/engine` },
    ],
  },
]
</script>
