import { useTranslate } from '@tolgee/react';
import { exhaustiveMatchingGuard } from 'tg.fixtures/exhaustiveMatchingGuard';
import { StateType } from 'tg.constants/translationStates';

export type TranslationStateType = StateType | 'OUTDATED' | 'AUTO_TRANSLATED';

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

      case 'AUTO_TRANSLATED':
        return t('translation_state_auto_translated');

      default:
        return exhaustiveMatchingGuard(state);
    }
  };
}
