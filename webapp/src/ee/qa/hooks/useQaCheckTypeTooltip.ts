import { useTranslate } from '@tolgee/react';
import { components } from 'tg.service/apiSchema.generated';

type QaCheckType = components['schemas']['QaIssueModel']['type'];

export function useQaCheckTypeTooltip(type: QaCheckType): string | null {
  const { t } = useTranslate();
  switch (type) {
    case 'SPACES_MISMATCH':
      return t('qa_check_tooltip_spaces_mismatch');
    case 'UNMATCHED_NEWLINES':
      return t('qa_check_tooltip_unmatched_newlines');
    case 'CHARACTER_CASE_MISMATCH':
      return t('qa_check_tooltip_character_case_mismatch');
    case 'MISSING_NUMBERS':
      return t('qa_check_tooltip_missing_numbers');
    case 'SPELLING':
      return t('qa_check_tooltip_spelling');
    case 'GRAMMAR':
      return t('qa_check_tooltip_grammar');
    case 'REPEATED_WORDS':
      return t('qa_check_tooltip_repeated_words');
    case 'PUNCTUATION_MISMATCH':
      return t('qa_check_tooltip_punctuation_mismatch');
    case 'BRACKETS_MISMATCH':
      return t('qa_check_tooltip_brackets_mismatch');
    case 'BRACKETS_UNBALANCED':
      return t('qa_check_tooltip_brackets_unbalanced');
    case 'SPECIAL_CHARACTER_MISMATCH':
      return t('qa_check_tooltip_special_character_mismatch');
    case 'DIFFERENT_URLS':
      return t('qa_check_tooltip_different_urls');
    case 'INCONSISTENT_PLACEHOLDERS':
      return t('qa_check_tooltip_inconsistent_placeholders');
    case 'INCONSISTENT_HTML':
      return t('qa_check_tooltip_inconsistent_html');
    case 'HTML_SYNTAX':
      return t('qa_check_tooltip_html_syntax');
    default:
      return null;
  }
}
