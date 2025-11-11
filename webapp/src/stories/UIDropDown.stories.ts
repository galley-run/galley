import type { Meta, StoryObj } from '@storybook/vue3-vite';

import { fn } from 'storybook/test';
import UIDropDown from '@/components/FormField/UIDropDown.vue'
import { Calendar, User } from '@solar-icons/vue'
import { ref } from 'vue'

const meta = {
  title: 'Galley/FormField/UIDropDown',
  component: UIDropDown,
  tags: ['autodocs'],
  argTypes: {
    variant: {
      control: 'select',
      options: ['inline', 'default', 'leadingAddon', 'trailingAddon', 'both', 'icon']
    },
    items: { control: 'object' },
    placeholder: { control: 'text' },
    selectFirst: { control: 'boolean' },
    disabled: { control: 'boolean' },
    maxHeightPx: { control: 'number' },
    menuPosition: { control: 'select', options: ['left', 'right'] },
  },
  args: {
    'onUpdate:modelValue': fn(),
    onChange: fn(),
  },
} satisfies Meta<typeof UIDropDown>;

export default meta;
type Story = StoryObj<typeof meta>;

const sampleItems = [
  { label: 'Option 1', value: 'option1' },
  { label: 'Option 2', value: 'option2' },
  { label: 'Option 3', value: 'option3' },
  { label: 'Disabled Option', value: 'option4', disabled: true },
];

export const Default: Story = {
  args: {
    items: sampleItems,
    placeholder: 'Select an option...',
  },
  render: (args) => ({
    components: { UIDropDown },
    setup() {
      const model = ref(null);
      return { args, model };
    },
    template: '<UIDropDown v-bind="args" v-model="model" />',
  }),
};

export const WithPreselected: Story = {
  args: {
    items: sampleItems,
    placeholder: 'Select an option...',
  },
  render: (args) => ({
    components: { UIDropDown },
    setup() {
      const model = ref('option2');
      return { args, model };
    },
    template: '<UIDropDown v-bind="args" v-model="model" />',
  }),
};

export const Inline: Story = {
  args: {
    items: sampleItems,
    variant: 'inline',
    placeholder: 'Select...',
  },
  render: (args) => ({
    components: { UIDropDown },
    setup() {
      const model = ref(null);
      return { args, model };
    },
    template: '<UIDropDown v-bind="args" v-model="model" />',
  }),
};

export const IconVariant: Story = {
  args: {
    items: sampleItems,
    variant: 'icon',
    icon: User,
  },
  render: (args) => ({
    components: { UIDropDown },
    setup() {
      const model = ref(null);
      return { args, model };
    },
    template: '<UIDropDown v-bind="args" v-model="model" />',
  }),
};

export const WithLinks: Story = {
  args: {
    items: [
      { label: 'Dashboard', value: '/dashboard', link: true },
      { label: 'Settings', value: '/settings', link: true },
      { label: 'External Link', value: 'https://example.com', link: 'external' },
    ],
    placeholder: 'Navigate to...',
  },
  render: (args) => ({
    components: { UIDropDown },
    setup() {
      const model = ref(null);
      return { args, model };
    },
    template: '<UIDropDown v-bind="args" v-model="model" />',
  }),
};

export const Disabled: Story = {
  args: {
    items: sampleItems,
    placeholder: 'Select an option...',
    disabled: true,
  },
  render: (args) => ({
    components: { UIDropDown },
    setup() {
      const model = ref(null);
      return { args, model };
    },
    template: '<UIDropDown v-bind="args" v-model="model" />',
  }),
};

export const SelectFirst: Story = {
  args: {
    items: sampleItems,
    placeholder: 'Select an option...',
    selectFirst: true,
  },
  render: (args) => ({
    components: { UIDropDown },
    setup() {
      const model = ref(null);
      return { args, model };
    },
    template: '<UIDropDown v-bind="args" v-model="model" />',
  }),
};

export const RightAlignedMenu: Story = {
  args: {
    items: sampleItems,
    placeholder: 'Select an option...',
    menuPosition: 'right',
  },
  render: (args) => ({
    components: { UIDropDown },
    setup() {
      const model = ref(null);
      return { args, model };
    },
    template: '<div style="margin-left: 200px;"><UIDropDown v-bind="args" v-model="model" /></div>',
  }),
};

export const DestructiveItems: Story = {
  args: {
    items: [
      { label: 'View', value: 'view' },
      { label: 'Edit', value: 'edit' },
      { label: 'Delete', value: 'delete', variant: 'destructive' },
    ],
    placeholder: 'Actions...',
  },
  render: (args) => ({
    components: { UIDropDown },
    setup() {
      const model = ref(null);
      return { args, model };
    },
    template: '<UIDropDown v-bind="args" v-model="model" />',
  }),
};

export const WithLeadingAddon: Story = {
  args: {
    items: sampleItems,
    variant: 'leadingAddon',
    placeholder: 'Select an option...',
  },
  render: (args) => ({
    components: { UIDropDown, User },
    setup() {
      const model = ref(null);
      return { args, model, User };
    },
    template: `
      <UIDropDown v-bind="args" v-model="model">
        <template #leadingAddon>
          <User :size="20" />
        </template>
      </UIDropDown>
    `,
  }),
};

export const WithTrailingAddon: Story = {
  args: {
    items: sampleItems,
    variant: 'trailingAddon',
    placeholder: 'Select an option...',
  },
  render: (args) => ({
    components: { UIDropDown, Calendar },
    setup() {
      const model = ref(null);
      return { args, model, Calendar };
    },
    template: `
      <UIDropDown v-bind="args" v-model="model">
        <template #trailingAddon>
          <Calendar :size="20" />
        </template>
      </UIDropDown>
    `,
  }),
};

export const WithBothAddons: Story = {
  args: {
    items: sampleItems,
    variant: 'both',
    placeholder: 'Select an option...',
  },
  render: (args) => ({
    components: { UIDropDown, User, Calendar },
    setup() {
      const model = ref(null);
      return { args, model, User, Calendar };
    },
    template: `
      <UIDropDown v-bind="args" v-model="model">
        <template #leadingAddon>
          <User :size="20" />
        </template>
        <template #trailingAddon>
          <Calendar :size="20" />
        </template>
      </UIDropDown>
    `,
  }),
};
