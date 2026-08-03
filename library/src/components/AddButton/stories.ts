import type { Meta, StoryObj } from '@storybook/react-vite';
import { AddButton } from '.';

const meta = {
  component: AddButton,
  parameters: {
    layout: 'centered',
  },
  args: {
    children: 'Add key',
  },
  tags: ['autodocs'],
} satisfies Meta<typeof AddButton>;

export default meta;

type Story = StoryObj<typeof meta>;

export const Default = {
  args: {},
} satisfies Story;

export const Disabled = {
  args: {
    disabled: true,
  },
} satisfies Story;
