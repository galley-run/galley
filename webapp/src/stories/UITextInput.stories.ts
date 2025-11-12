import type { Meta, StoryObj } from '@storybook/vue3-vite';

import { fn } from 'storybook/test';
import UITextInput from '@/components/FormField/UITextInput.vue'
import { Magnifer, Calendar } from '@solar-icons/vue'
import { ref } from 'vue'

const meta = {
  title: 'Galley/FormField/UITextInput',
  component: UITextInput,
  tags: ['autodocs'],
  argTypes: {
    leadingAddon: { control: false },
    trailingAddon: { control: false },
  },
  args: {
    onInput: fn(),
    onChange: fn(),
  },
} satisfies Meta<typeof UITextInput>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {},
  render: (args) => ({
    components: { UITextInput },
    setup() {
      const model = ref('');
      return { args, model };
    },
    template: '<UITextInput v-bind="args" v-model="model" placeholder="Enter text..." />',
  }),
};

export const WithLeadingIcon: Story = {
  args: {
    leadingAddon: Magnifer,
  },
  render: (args) => ({
    components: { UITextInput },
    setup() {
      const model = ref('');
      return { args, model };
    },
    template: '<UITextInput v-bind="args" v-model="model" placeholder="Search..." />',
  }),
};

export const WithTrailingIcon: Story = {
  args: {
    trailingAddon: Calendar,
  },
  render: (args) => ({
    components: { UITextInput },
    setup() {
      const model = ref('');
      return { args, model };
    },
    template: '<UITextInput v-bind="args" v-model="model" placeholder="Select date..." />',
  }),
};

export const WithLeadingText: Story = {
  args: {
    leadingAddon: '$',
  },
  render: (args) => ({
    components: { UITextInput },
    setup() {
      const model = ref('');
      return { args, model };
    },
    template: '<UITextInput v-bind="args" v-model="model" placeholder="0.00" />',
  }),
};

export const WithTrailingText: Story = {
  args: {
    trailingAddon: 'USD',
  },
  render: (args) => ({
    components: { UITextInput },
    setup() {
      const model = ref('');
      return { args, model };
    },
    template: '<UITextInput v-bind="args" v-model="model" placeholder="0.00" />',
  }),
};

export const Disabled: Story = {
  args: {},
  render: (args) => ({
    components: { UITextInput },
    setup() {
      const model = ref('Disabled value');
      return { args, model };
    },
    template: '<UITextInput v-bind="args" v-model="model" disabled />',
  }),
};
