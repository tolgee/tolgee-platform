import { components } from 'tg.service/apiSchema.generated';

export type BatchActions =
  | 'delete'
  | 'machine_translate'
  | 'pre_translate'
  | 'mark_as_reviewed'
  | 'mark_as_translated'
  | 'add_tags'
  | 'remove_tags'
  | 'change_namespace'
  | 'copy_translations'
  | 'clear_translations'
  | 'export_translations';

export type BatchJobModel = components['schemas']['BatchJobModel'];

export interface OperationProps {
  disabled: boolean;
  onStart: (operation: BatchJobModel) => void;
  onClose: () => void;
  onFinished: () => void;
}

export type BatchJobStatus = components['schemas']['BatchJobModel']['status'];
