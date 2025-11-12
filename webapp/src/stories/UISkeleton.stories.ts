import type { Meta, StoryObj } from '@storybook/vue3-vite';

import UISkeleton from '@/components/FormField/UISkeleton.vue'

const meta = {
  title: 'Galley/FormField/UISkeleton',
  component: UISkeleton,
  tags: ['autodocs'],
  argTypes: {},
  args: {},
} satisfies Meta<typeof UISkeleton>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {},
  render: (args) => ({
    components: { UISkeleton },
    setup() {
      return { args };
    },
    template: '<UISkeleton v-bind="args" />',
  }),
};

export const Multiple: Story = {
  render: () => ({
    components: { UISkeleton },
    template: `
      <div style="display: flex; flex-direction: column; gap: 8px;">
        <UISkeleton />
        <UISkeleton />
        <UISkeleton />
      </div>
    `,
  }),
};

export const WithCustomWidth: Story = {
  render: () => ({
    components: { UISkeleton },
    template: `
      <div style="display: flex; flex-direction: column; gap: 8px;">
        <UISkeleton style="width: 100%;" />
        <UISkeleton style="width: 75%;" />
        <UISkeleton style="width: 50%;" />
      </div>
    `,
  }),
};
