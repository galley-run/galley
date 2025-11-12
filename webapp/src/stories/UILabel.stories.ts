import type { Meta, StoryObj } from '@storybook/vue3-vite';

import { fn } from 'storybook/test';
import UILabel from '@/components/FormField/UILabel.vue'

const meta = {
  title: 'Galley/FormField/UILabel',
  component: UILabel,
  tags: ['autodocs'],
  argTypes: {
    for: { control: 'text' },
    required: { control: 'boolean' },
  },
  args: {
    onInfoClick: fn(),
  },
} satisfies Meta<typeof UILabel>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {
    for: 'example-input',
  },
  render: (args) => ({
    components: { UILabel },
    setup() {
      return { args };
    },
    template: '<UILabel v-bind="args">Label Text</UILabel>',
  }),
};

export const Required: Story = {
  args: {
    for: 'example-input',
    required: true,
  },
  render: (args) => ({
    components: { UILabel },
    setup() {
      return { args };
    },
    template: '<UILabel v-bind="args">Label Text</UILabel>',
  }),
};

export const WithInfoButton: Story = {
  args: {
    for: 'example-input',
  },
  render: (args) => ({
    components: { UILabel },
    setup() {
      const onInfoClick = fn();
      return { args, onInfoClick };
    },
    template: '<UILabel v-bind="args" @info-click="onInfoClick">Label Text</UILabel>',
  }),
};
