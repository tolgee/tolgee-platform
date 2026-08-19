import type { Meta, StoryObj } from '@storybook/react-vite';
import { FlagImage } from '.';

const meta = {
  title: 'Components/Languages/FlagImage',
  component: FlagImage,
  parameters: {
    layout: 'centered',
  },
} satisfies Meta<typeof FlagImage>;

export default meta;

type Story = StoryObj<typeof meta>;

export const Supported = {
  args: {
    flagEmoji: '🇨🇿',
    width: 128,
  },
} satisfies Story;

export const Unsupported = {
  args: {
    flagEmoji: 'flag',
    width: 128,
  },
} satisfies Story;
