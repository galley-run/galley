import type { Meta, StoryObj } from '@storybook/vue3-vite';

import { fn } from 'storybook/test';
import UIConfetti from '@/components/UIConfetti.vue'

const meta = {
  title: 'Galley/UIConfetti',
  component: UIConfetti,
  tags: ['autodocs'],
  argTypes: {
    autoplay: { control: 'boolean' },
    particles: { control: 'number' },
    durationMs: { control: 'number' },
    colors: { control: 'object' },
    origin: { control: 'object' },
  },
  args: {},
} satisfies Meta<typeof UIConfetti>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {
    autoplay: true,
  },
  render: (args) => ({
    components: { UIConfetti },
    setup() {
      return { args };
    },
    template: '<div style="width: 100%; height: 400px;"><UIConfetti v-bind="args" /></div>',
  }),
};

export const CustomColors: Story = {
  args: {
    autoplay: true,
    colors: ['#FF0000', '#00FF00', '#0000FF'],
  },
  render: (args) => ({
    components: { UIConfetti },
    setup() {
      return { args };
    },
    template: '<div style="width: 100%; height: 400px;"><UIConfetti v-bind="args" /></div>',
  }),
};

export const MoreParticles: Story = {
  args: {
    autoplay: true,
    particles: 500,
  },
  render: (args) => ({
    components: { UIConfetti },
    setup() {
      return { args };
    },
    template: '<div style="width: 100%; height: 400px;"><UIConfetti v-bind="args" /></div>',
  }),
};

export const LongerDuration: Story = {
  args: {
    autoplay: true,
    durationMs: 3000,
  },
  render: (args) => ({
    components: { UIConfetti },
    setup() {
      return { args };
    },
    template: '<div style="width: 100%; height: 400px;"><UIConfetti v-bind="args" /></div>',
  }),
};
