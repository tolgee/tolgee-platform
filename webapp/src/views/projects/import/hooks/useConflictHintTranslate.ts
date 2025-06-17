import { useTranslate } from '@tolgee/react';
import { exhaustiveMatchingGuard } from 'tg.fixtures/exhaustiveMatchingGuard';
import { components } from 'tg.service/apiSchema.generated';

type ConflictType =
  components['schemas']['ImportTranslationModel']['conflictType'];

export const useConflictHintTranslate = () => {
  const { t } = useTranslate();

  return (type: ConflictType) => {
    if (!type) {
      return undefined;
    }
    switch (type) {
      case 'CANNOT_EDIT_DISABLED':
        return t('conflict_cannot_edit_disabled_hint');
      case 'CANNOT_EDIT_REVIEWED':
        return t('conflict_cannot_edit_reviewed_hint');
      case 'SHOULD_NOT_EDIT_REVIEWED':
        return t('conflict_should_not_edit_reviewed_hint');
      default:
        return exhaustiveMatchingGuard(type);
    }
  };
};
