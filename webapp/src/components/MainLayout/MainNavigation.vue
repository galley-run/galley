<template>
  <aside class="main-navigation">
    <section v-for="(section, sIdx) in projectSections" :key="sIdx">
      <nav>
        <h6 class="text-tides-800 px-2 pb-2">{{ section.label }}</h6>
        <ul class="flex flex-col gap-0.5">
          <li v-for="(item, idx) in section.items" :key="idx">
            <RouterLink
              :to="item.href"
              exact-active-class="bg-navy-50"
              :tabindex="1"
            >
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
          <li
            v-for="(item, idx) in section.items"
            :key="idx"
          >
            <RouterLink
              :to="item.href"
              exact-active-class="bg-navy-50"
              :tabindex="1"
            >
              <component :is="item.icon" size="20" />
              {{ item.label }}
            </RouterLink>
          </li>
        </ul>
      </nav>
    </section>
  </aside>
</template>

<script setup lang="ts">
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

const projectSections = [
  {
    label: 'The Galley',
    items: [
      { icon: PresentationGraph, label: 'Dashboard', href: '/' },
      { icon: Routing3, label: 'Gateway', href: '/gateway' },
      { icon: Server2, label: 'Applications', href: '/applications' },
      { icon: Database, label: 'Databases', href: '/databases' },
      { icon: FolderWithFiles, label: 'Object Storage', href: '/object-storage' },
    ],
  },
  {
    label: 'The Pantry',
    items: [
      { icon: Password, label: 'Secrets', href: '/secrets' },
      { icon: ClipboardList, label: 'Configs', href: '/configs' },
    ],
  },
  {
    label: "Captain's Quarters",
    items: [
      { icon: CloudUpload, label: 'Releases', href: '/releases' },
      { icon: Graph, label: 'Monitoring', href: '/monitoring' },
    ],
  },
  {
    label: 'Below Deck',
    items: [
      { icon: LinkCircle, label: 'Networking', href: '/networking' },
      { icon: CodeFile, label: 'Automations', href: '/automations' },
      { icon: Settings, label: 'Settings', href: '/settings' },
    ],
  },
]

const vesselSections = [
  {
    label: 'Charter',
    items: [
      { icon: Settings, label: 'Settings', href: '/charter/settings' },
      { icon: WalletMoney, label: 'Billing', href: '/charter/billing' },
      { icon: ServerSquareCloud, label: 'Compute Plans', href: '/charter/compute-plans' },
      { icon: Diploma, label: 'Compliance', href: '/charter/compliance' },
      { icon: UsersGroupTwoRounded, label: 'Crew', href: '/charter/crew' },
    ],
  },
  {
    label: 'Vessel',
    items: [
      { icon: Settings, label: 'Settings', href: '/vessel/settings' },
      { icon: WalletMoney, label: 'Billing', href: '/vessel/billing' },
      { icon: ServerPath, label: 'Engine', href: '/vessel/engine' },
    ],
  },
]
</script>
