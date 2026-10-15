import { FeatureFlag } from 'tg.service/apiSchemaTypes';

export const hasFeatureFlag = (
  enabledFlags: FeatureFlag[] | undefined,
  flag: FeatureFlag
): boolean => Boolean(enabledFlags?.includes(flag));
