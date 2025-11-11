import type { Meta, StoryObj } from '@storybook/vue3-vite';

import UIFormField from '@/components/FormField/UIFormField.vue'
import UILabel from '@/components/FormField/UILabel.vue'
import UITextInput from '@/components/FormField/UITextInput.vue'

const meta = {
  title: 'Galley/FormField/UIFormField',
  component: UIFormField,
  tags: ['autodocs'],
  argTypes: {},
  args: {},
} satisfies Meta<typeof UIFormField>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  render: (args) => ({
    components: { UIFormField, UILabel, UITextInput },
    setup() {
      return { args };
    },
    template: `
      <UIFormField v-bind="args">
        <UILabel for="example">Example Field</UILabel>
        <UITextInput id="example" placeholder="Enter text..." />
      </UIFormField>
    `,
  }),
};

export const WithRequiredLabel: Story = {
  render: (args) => ({
    components: { UIFormField, UILabel, UITextInput },
    setup() {
      return { args };
    },
    template: `
      <UIFormField v-bind="args">
        <UILabel for="example" required>Example Field</UILabel>
        <UITextInput id="example" placeholder="Enter text..." />
      </UIFormField>
    `,
  }),
};
