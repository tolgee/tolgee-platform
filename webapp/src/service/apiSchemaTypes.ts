/**
 * Complementary types to `apiSchemaTypes.generated.ts`.
 * Since not all types get to exist in the root of `components['schemas']`,
 * we extract them manually here.
 */

import {
  FeatureFlagStateModel,
  QaSettingsRequest,
  QaIssueModel,
  SelfHostedEePlanModel,
} from 'tg.service/apiSchemaTypes.generated';

export type Feature = SelfHostedEePlanModel['enabledFeatures'][number];
export type FeatureFlag = FeatureFlagStateModel['flag'];
export type QaCheckType = QaIssueModel['type'];
export type QaCheckSeverity =
  QaSettingsRequest['settings'][keyof QaSettingsRequest['settings']];
