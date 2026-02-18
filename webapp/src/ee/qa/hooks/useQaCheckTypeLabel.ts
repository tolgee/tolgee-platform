import { useTranslate } from '@tolgee/react';

export function useQaCheckTypeLabel(type: string): string {
  const { t } = useTranslate();
  switch (type) {
    case 'EMPTY_TRANSLATION':
      return t('qa_check_type_empty_translation');
    default:
      return type;
  }
}
