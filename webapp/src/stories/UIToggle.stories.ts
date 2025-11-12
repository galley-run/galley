import type { Meta, StoryObj } from '@storybook/vue3-vite'

import { fn } from 'storybook/test'
import UIToggle from '@/components/FormField/UIToggle.vue'
import { ref } from 'vue'

const meta = {
  title: 'Galley/FormField/UIToggle',
  component: UIToggle,
  tags: ['autodocs'],
  argTypes: {
    label: { control: 'text' },
  },
  args: {
    onInput: fn(),
    onChange: fn(),
  },
} satisfies Meta<typeof UIToggle>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {
  args: {
    label: 'Option 1',
  },
  render: (args) => ({
    components: { UIToggle },
    setup() {
      const model = ref('')
      return { args, model }
    },
    template: '<UIToggle v-bind="args" v-model="model" value="option1" />',
  }),
}

export const RadioButtonGroup: Story = {
  args: {
    label: '',
  },
  render: () => ({
    components: { UIToggle },
    setup() {
      const selected = ref(['option1'])
      return { selected }
    },
    template: `
      <div style="display: flex; flex-direction: column; gap: 16px;">
        <UIToggle
          v-model="selected"
          value="option1"
          label="Option 1"
        />
        <UIToggle
          v-model="selected"
          value="option2"
          label="Option 2"
        />
        <UIToggle
          v-model="selected"
          value="option3"
          label="Option 3"
        />
      </div>
    `,
  }),
}
