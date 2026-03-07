import { useTranslate } from '@tolgee/react';
import type { QaCheckResultItem } from './useQaCheckPreview';

export function useQaCheckTypeLabel(type: QaCheckResultItem['type']): string {
  const { t } = useTranslate();
  switch (type) {
    case 'EMPTY_TRANSLATION':
      return t('qa_check_type_empty_translation');
    case 'SPACES_MISMATCH':
      return t('qa_check_type_spaces_mismatch');
    case 'UNMATCHED_NEWLINES':
      return t('qa_check_type_unmatched_newlines');
    case 'CHARACTER_CASE_MISMATCH':
      return t('qa_check_type_character_case_mismatch');
    case 'MISSING_NUMBERS':
      return t('qa_check_type_missing_numbers');
    case 'SPELLING':
      return t('qa_check_type_spelling');
    case 'REPEATED_WORDS':
      return t('qa_check_type_repeated_words');
    case 'PUNCTUATION_MISMATCH':
      return t('qa_check_type_punctuation_mismatch');
    case 'BRACKETS_MISMATCH':
      return t('qa_check_type_brackets_mismatch');
    case 'SPECIAL_CHARACTER_MISMATCH':
      return t('qa_check_type_special_character_mismatch');
    case 'DIFFERENT_URLS':
      return t('qa_check_type_different_urls');
    case 'INCONSISTENT_PLACEHOLDERS':
      return t('qa_check_type_inconsistent_placeholders');
    case 'INCONSISTENT_HTML':
      return t('qa_check_type_inconsistent_html');
    case 'ICU_SYNTAX':
      return t('qa_check_type_icu_syntax');
    case 'UNRESOLVED_COMMENTS':
      return t('qa_check_type_unresolved_comments');
    default:
      return type;
  }
}
