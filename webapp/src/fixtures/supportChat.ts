import { Feature } from 'tg.service/apiSchemaTypes';

const SUPPORT_CHAT_FEATURES: Feature[] = [
  'STANDARD_SUPPORT',
  'PREMIUM_SUPPORT',
];

type Params = {
  limitedView: boolean | undefined;
  features: Feature[];
};

/** `limitedView` marks a viewer who reaches the organization only through its public projects. */
export const hasSupportChat = ({ limitedView, features }: Params): boolean =>
  !limitedView &&
  SUPPORT_CHAT_FEATURES.some((feature) => features.includes(feature));
