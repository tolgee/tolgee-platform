import { components } from 'tg.service/apiSchema.generated';

export type BatchActions =
  | 'delete'
  | 'machine_translate'
  | 'pre_translate'
  | 'mark_as_reviewed'
  | 'mark_as_translated'
  | 'task_create'
  | 'task_add_keys'
  | 'task_remove_keys'
  | 'order_translation'
  | 'add_tags'
  | 'remove_tags'
  | 'change_namespace'
  | 'copy_translations'
  | 'clear_translations'
  | 'export_translations'
  | 'assign_translation_labels'
  | 'unassign_translation_labels';

export type BatchJobModel = components['schemas']['BatchJobModel'];

export interface OperationProps {
  disabled: boolean;
  onStart: (operation: BatchJobModel) => void;
  onClose: () => void;
  onFinished: () => void;
}

export type BatchJobStatus = components['schemas']['BatchJobModel']['status'];
