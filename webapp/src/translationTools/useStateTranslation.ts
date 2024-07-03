import { useTranslate } from '@tolgee/react';
import { TranslationStateType } from 'tg.ee/task/components/taskCreate/TranslationStateFilter';
import { exhaustiveMatchingGuard } from 'tg.fixtures/exhaustiveMatchingGuard';

export function useStateTranslation() {
  const { t } = useTranslate();
  return function (state: TranslationStateType) {
    switch (state) {
      case 'UNTRANSLATED':
        return t('translation_state_untranslated');

      case 'TRANSLATED':
        return t('translation_state_translated');

      case 'REVIEWED':
        return t('translation_state_reviewed');

      case 'DISABLED':
        return t('translation_state_disabled');

      case 'OUTDATED':
        return t('translation_state_outdated');

      default:
        return exhaustiveMatchingGuard(state);
    }
  };
}
