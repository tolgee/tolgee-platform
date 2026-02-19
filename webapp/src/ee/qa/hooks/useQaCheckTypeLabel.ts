import { useTranslate } from '@tolgee/react';

export function useQaCheckTypeLabel(type: string): string {
  const { t } = useTranslate();
  switch (type) {
    case 'EMPTY_TRANSLATION':
      return t('qa_check_type_empty_translation');
    case 'SPACES_MISMATCH':
      return t('qa_check_type_spaces_mismatch');
    case 'PUNCTUATION_MISMATCH':
      return t('qa_check_type_punctuation_mismatch');
    case 'CHARACTER_CASE_MISMATCH':
      return t('qa_check_type_character_case_mismatch');
    case 'MISSING_NUMBERS':
      return t('qa_check_type_missing_numbers');
    default:
      return type;
  }
}
