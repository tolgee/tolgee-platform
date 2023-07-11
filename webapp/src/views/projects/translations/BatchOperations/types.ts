import { components } from 'tg.service/apiSchema.generated';

export type BatchActions =
  | 'delete'
  | 'auto_translate'
  | 'mark_as_reviewed'
  | 'mark_as_translated'
  | 'add_tags'
  | 'remove_tags'
  | 'change_namespace'
  | 'copy_translations'
  | 'clear_translations';

export type BatchJobModel = components['schemas']['BatchJobModel'];

export interface OperationProps {
  disabled: boolean;
  onStart: (operation: BatchJobModel) => void;
}

export type BatchJobStatus = components['schemas']['BatchJobModel']['status'];
