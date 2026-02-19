import { useTranslate } from '@tolgee/react';

export function useQaIssueMessage(
  message: string,
  params?: Record<string, string>
): string {
  const { t } = useTranslate();
  switch (message) {
    case 'qa_empty_translation':
      return t('qa_issue_empty_translation');
    case 'qa_check_failed':
      return t('qa_check_failed');
    case 'qa_spaces_leading_added':
      return t('qa_issue_spaces_leading_added');
    case 'qa_spaces_leading_removed':
      return t('qa_issue_spaces_leading_removed');
    case 'qa_spaces_trailing_added':
      return t('qa_issue_spaces_trailing_added');
    case 'qa_spaces_trailing_removed':
      return t('qa_issue_spaces_trailing_removed');
    case 'qa_spaces_doubled':
      return t('qa_issue_spaces_doubled');
    case 'qa_spaces_non_breaking_added':
      return t('qa_issue_spaces_non_breaking_added');
    case 'qa_spaces_non_breaking_removed':
      return t('qa_issue_spaces_non_breaking_removed');
    case 'qa_punctuation_add':
      return t('qa_issue_punctuation_add', params);
    case 'qa_punctuation_remove':
      return t('qa_issue_punctuation_remove', params);
    case 'qa_punctuation_replace':
      return t('qa_issue_punctuation_replace', params);
    case 'qa_case_capitalize':
      return t('qa_issue_case_capitalize');
    case 'qa_case_lowercase':
      return t('qa_issue_case_lowercase');
    case 'qa_numbers_missing':
      return params?.number
        ? t('qa_issue_numbers_missing', params)
        : t('qa_issue_numbers_missing_no_params');
    default:
      return message;
  }
}
