import { useTranslate } from '@tolgee/react';
import { exhaustiveMatchingGuard } from 'tg.fixtures/exhaustiveMatchingGuard';
import { components } from 'tg.service/apiSchema.generated';

type State = components['schemas']['TranslationViewModel']['state'];

export function useStateTranslation() {
  const { t } = useTranslate();
  return function (state: State) {
    switch (state) {
      case 'UNTRANSLATED':
        return t('translation_state_untranslated');

      case 'TRANSLATED':
        return t('translation_state_translated');

      case 'REVIEWED':
        return t('translation_state_reviewed');

      case 'DISABLED':
        return t('translation_state_disabled');

      default:
        exhaustiveMatchingGuard(state);
    }
  };
}
