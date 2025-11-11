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
    variant: { control: 'select', options: ['primary', 'neutral', 'custom', 'icon', 'destructive'] },
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
export const Regular: Story = {
  render: () => ({
    components: { UIButton },
    template: `
      <div style="display: flex; gap: 12px; align-items: center;">
        <UIButton variant="primary">Primary</UIButton>
        <UIButton variant="neutral">Neutral</UIButton>
        <UIButton variant="destructive">Destructive</UIButton>
      </div>
    `,
  }),
};

export const Ghost: Story = {
  render: () => ({
    components: { UIButton },
    template: `
      <div style="display: flex; gap: 12px; align-items: center;">
        <UIButton variant="primary" ghost>Primary</UIButton>
        <UIButton variant="neutral" ghost>Neutral</UIButton>
        <UIButton variant="destructive" ghost>Destructive</UIButton>
      </div>
    `,
  }),
};

export const Small: Story = {
  render: () => ({
    components: { UIButton },
    template: `
      <div style="display: flex; gap: 12px; align-items: center;">
        <UIButton variant="primary" small>Primary</UIButton>
        <UIButton variant="neutral" small>Neutral</UIButton>
        <UIButton variant="destructive" small>Destructive</UIButton>
      </div>
    `,
  }),
};

export const SmallGhost: Story = {
  render: () => ({
    components: { UIButton },
    template: `
      <div style="display: flex; gap: 12px; align-items: center;">
        <UIButton variant="primary" small ghost>Primary</UIButton>
        <UIButton variant="neutral" small ghost>Neutral</UIButton>
        <UIButton variant="destructive" small ghost>Destructive</UIButton>
      </div>
    `,
  }),
};

export const Large: Story = {
  render: () => ({
    components: { UIButton },
    template: `
      <div style="display: flex; gap: 12px; align-items: center;">
        <UIButton variant="primary" large>Primary</UIButton>
        <UIButton variant="neutral" large>Neutral</UIButton>
        <UIButton variant="destructive" large>Destructive</UIButton>
      </div>
    `,
  }),
};

export const LargeGhost: Story = {
  render: () => ({
    components: { UIButton },
    template: `
      <div style="display: flex; gap: 12px; align-items: center;">
        <UIButton variant="primary" large ghost>Primary</UIButton>
        <UIButton variant="neutral" large ghost>Neutral</UIButton>
        <UIButton variant="destructive" large ghost>Destructive</UIButton>
      </div>
    `,
  }),
};
