import { components } from 'tg.service/apiSchema.generated';

export type BatchActions = 'delete' | 'translate';

export type BatchJobModel = components['schemas']['BatchJobModel'];

export interface OperationProps {
  disabled: boolean;
  onStart: (operation: BatchJobModel) => void;
}
