import { useTranslate } from '@tolgee/react';
import { exhaustiveMatchingGuard } from 'tg.fixtures/exhaustiveMatchingGuard';
import { components } from 'tg.service/apiSchema.generated';

type LanguageConfigItemModel = components['schemas']['LanguageConfigItemModel'];
type FormalityType =
  LanguageConfigItemModel['enabledServicesInfo'][number]['formality'];

export function useFormalityTranslation() {
  const { t } = useTranslate();

  return (formality: FormalityType) => {
    switch (formality) {
      case undefined:
      case 'DEFAULT':
        return t('mt_formality_default');
      case 'FORMAL':
        return t('mt_formality_formal');
      case 'INFORMAL':
        return t('mt_formality_informal');
      default:
        return exhaustiveMatchingGuard(formality);
    }
  };
}
