import type { Meta, StoryObj } from '@storybook/react-vite';
import { MfaBadge } from '.';

const meta = {
  component: MfaBadge,
  parameters: {
    layout: 'centered',
  },
  play: async ({ canvas, userEvent }) => {
    setTimeout(() => userEvent.hover(canvas.getAllByText(() => true)[1]), 500);
  },
  tags: ['autodocs'],
} satisfies Meta<typeof MfaBadge>;

export default meta;

type Story = StoryObj<typeof meta>;

export const Enabled = {
  args: {
    enabled: true,
  },
} satisfies Story;

export const Disabled = {
  args: {
    enabled: false,
  },
} satisfies Story;
