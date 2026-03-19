import { useTranslate } from '@tolgee/react';

export function useQaIssueMessage(
  message: string,
  params?: Record<string, string> | null | undefined
): string {
  const normalizedParams = params ?? undefined;
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
      return t('qa_issue_punctuation_add', normalizedParams);
    case 'qa_punctuation_remove':
      return t('qa_issue_punctuation_remove', normalizedParams);
    case 'qa_punctuation_replace':
      return t('qa_issue_punctuation_replace', normalizedParams);
    case 'qa_case_capitalize':
      return t('qa_issue_case_capitalize');
    case 'qa_case_lowercase':
      return t('qa_issue_case_lowercase');
    case 'qa_numbers_missing':
      return normalizedParams?.number
        ? t('qa_issue_numbers_missing', normalizedParams)
        : t('qa_issue_numbers_missing_no_params');
    case 'qa_newlines_missing':
      return t('qa_issue_newlines_missing', normalizedParams);
    case 'qa_newlines_extra':
      return t('qa_issue_newlines_extra', normalizedParams);
    case 'qa_newlines_too_many_sections':
      return t('qa_issue_newlines_too_many_sections', normalizedParams);
    case 'qa_newlines_too_few_sections':
      return t('qa_issue_newlines_too_few_sections', normalizedParams);
    case 'qa_special_char_missing':
      return normalizedParams?.character
        ? t('qa_issue_special_char_missing', normalizedParams)
        : t('qa_issue_special_char_missing_no_params');
    case 'qa_special_char_added':
      return normalizedParams?.character
        ? t('qa_issue_special_char_added', normalizedParams)
        : t('qa_issue_special_char_added_no_params');
    case 'qa_url_missing':
      return normalizedParams?.url
        ? t('qa_issue_url_missing', normalizedParams)
        : t('qa_issue_url_missing_no_params');
    case 'qa_url_extra':
      return normalizedParams?.url
        ? t('qa_issue_url_extra', normalizedParams)
        : t('qa_issue_url_extra_no_params');
    case 'qa_url_replace':
      return normalizedParams?.url && normalizedParams?.expected
        ? t('qa_issue_url_replace', normalizedParams)
        : t('qa_issue_url_replace_no_params');
    default:
      return message;
  }
}
