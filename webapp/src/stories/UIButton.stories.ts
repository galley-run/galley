import type { Meta, StoryObj } from '@storybook/vue3-vite';

import { fn } from 'storybook/test';
import UIButton from '@/components/UIButton.vue'

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories
const meta = {
  title: 'Galley/UIButton',
  component: UIButton,
  // This component will have an automatically generated docsPage entry: https://storybook.js.org/docs/writing-docs/autodocs
  tags: ['autodocs'],
  argTypes: {
    variant: { control: 'select', options: ['primary', 'neutral', 'custom', 'icon'] },
    large: { control: 'boolean' },
    small: { control: 'boolean' },
    ghost: { control: 'boolean' },
  },
  args: {
    // Use `fn` to spy on the onClick arg, which will appear in the actions panel once invoked: https://storybook.js.org/docs/essentials/actions#story-args
    onClick: fn(),
  },
} satisfies Meta<typeof UIButton>;

export default meta;
type Story = StoryObj<typeof meta>;
/*
 *👇 Render functions are a framework specific feature to allow you control on how the component renders.
 * See https://storybook.js.org/docs/api/csf
 * to learn how to use render functions.
 */
export const Primary: Story = {
  args: {
    variant: 'primary',
  },
  render: (args) => ({
    components: { UIButton },
    setup() {
      return { args };
    },
    template: '<UIButton v-bind="args">Button</UIButton>',
  }),
};

export const Neutral: Story = {
  args: {
    variant: 'neutral',
  },
  render: (args) => ({
    components: { UIButton },
    setup() {
      return { args };
    },
    template: '<UIButton v-bind="args">Button</UIButton>',
  }),
};

export const Large: Story = {
  args: {
    large: true,
  },
  render: (args) => ({
    components: { UIButton },
    setup() {
      return { args };
    },
    template: '<UIButton v-bind="args">Button</UIButton>',
  }),
};

export const Small: Story = {
  args: {
    small: true,
  },
  render: (args) => ({
    components: { UIButton },
    setup() {
      return { args };
    },
    template: '<UIButton v-bind="args">Button</UIButton>',
  }),
};

export const Ghost: Story = {
  args: {
    ghost: true,
  },
  render: (args) => ({
    components: { UIButton },
    setup() {
      return { args };
    },
    template: '<UIButton v-bind="args">Button</UIButton>',
  }),
};
