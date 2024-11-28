import { components } from 'tg.service/apiSchema.generated';

export type TaskModel = components['schemas']['TaskModel'];
export type TaskState = TaskModel['state'];

export const TASK_ACTIVE_STATES = ['NEW', 'IN_PROGRESS'] as TaskState[];
