import type { Meta, StoryObj } from '@storybook/vue3-vite'

import { fn } from 'storybook/test'
import UIRadioButton from '@/components/FormField/UIRadioButton.vue'
import { ref } from 'vue'

const meta = {
  title: 'Galley/FormField/UIRadioButton',
  component: UIRadioButton,
  tags: ['autodocs'],
  argTypes: {
    label: { control: 'text' },
    description: { control: 'text' },
  },
  args: {
    onInput: fn(),
    onChange: fn(),
  },
} satisfies Meta<typeof UIRadioButton>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {
  args: {
    label: 'Option 1',
    description: 'This is the first option',
  },
  render: (args) => ({
    components: { UIRadioButton },
    setup() {
      const model = ref('')
      return { args, model }
    },
    template: '<UIRadioButton v-bind="args" v-model="model" value="option1" />',
  }),
}

export const RadioButtonGroup: Story = {
  args: {
    label: '',
    description: '',
  },
  render: () => ({
    components: { UIRadioButton },
    setup() {
      const selected = ref('option1')
      return { selected }
    },
    template: `
      <div style="display: flex; flex-direction: column; gap: 16px;">
        <UIRadioButton
          v-model="selected"
          value="option1"
          label="Option 1"
          description="This is the first option"
        />
        <UIRadioButton
          v-model="selected"
          value="option2"
          label="Option 2"
          description="This is the second option"
        />
        <UIRadioButton
          v-model="selected"
          value="option3"
          label="Option 3"
          description="This is the third option"
        />
      </div>
    `,
  }),
}
