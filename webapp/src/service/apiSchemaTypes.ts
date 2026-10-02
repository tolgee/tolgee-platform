/**
 * Complementary types to `apiSchemaTypes.generated.ts`.
 * Since not all types get to exist in the root of `components['schemas']`,
 * we extract them manually here.
 */

import {
  QaSettingsRequest,
  QaIssueModel,
  SelfHostedEePlanModel,
  TaskModel,
} from 'tg.service/apiSchemaTypes.generated';

export type Feature = SelfHostedEePlanModel['enabledFeatures'][number];
export type QaCheckType = QaIssueModel['type'];
export type TaskType = TaskModel['type'];
const TASK_TYPE_KEYS: Record<TaskType, true> = {
  TRANSLATE: true,
  REVIEW: true,
};
export const TASK_TYPES = Object.keys(TASK_TYPE_KEYS) as TaskType[];
export type QaCheckSeverity =
  QaSettingsRequest['settings'][keyof QaSettingsRequest['settings']];
