import type { Meta, StoryObj } from '@storybook/vue3-vite'

import { fn } from 'storybook/test'
import UIRadioCard from '@/components/FormField/UIRadioCard.vue'
import { ref } from 'vue'

const meta = {
  title: 'Galley/FormField/UIRadioCard',
  component: UIRadioCard,
  tags: ['autodocs'],
  argTypes: {
    title: { control: 'text' },
    description: { control: 'text' },
  },
  args: {
    onInput: fn(),
    onChange: fn(),
  },
} satisfies Meta<typeof UIRadioCard>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {
  args: {
    title: 'Option 1',
    description: 'This is the first option',
  },
  render: (args) => ({
    components: { UIRadioCard },
    setup() {
      const model = ref('')
      return { args, model }
    },
    template: '<UIRadioCard v-bind="args" v-model="model" value="option1" />',
  }),
}

export const Selected: Story = {
  args: {
    title: 'Option 1',
    description: 'This is the first option',
  },
  render: (args) => ({
    components: { UIRadioCard },
    setup() {
      const model = ref('option1')
      return { args, model }
    },
    template: '<UIRadioCard v-bind="args" v-model="model" value="option1" />',
  }),
}

export const Disabled: Story = {
  args: {
    title: 'Disabled Option',
    description: 'This option is disabled',
  },
  render: (args) => ({
    components: { UIRadioCard },
    setup() {
      const model = ref('')
      return { args, model }
    },
    template: '<UIRadioCard v-bind="args" v-model="model" value="option1" disabled />',
  }),
}

export const RadioGroup: Story = {
  args: {
    title: '',
    description: '',
  },
  render: () => ({
    components: { UIRadioCard },
    setup() {
      const selected = ref('option1')
      return { selected }
    },
    template: `
      <div style="display: flex; flex-direction: column; gap: 16px;">
        <UIRadioCard
          v-model="selected"
          value="option1"
          title="Option 1"
          description="This is the first option"
        />
        <UIRadioCard
          v-model="selected"
          value="option2"
          title="Option 2"
          description="This is the second option"
        />
        <UIRadioCard
          v-model="selected"
          value="option3"
          title="Option 3"
          description="This is the third option"
        />
      </div>
    `,
  }),
}
