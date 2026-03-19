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
    case 'qa_brackets_missing':
      return normalizedParams?.bracket
        ? t('qa_issue_brackets_missing', normalizedParams)
        : t('qa_issue_brackets_missing_no_params');
    case 'qa_brackets_extra':
      return normalizedParams?.bracket
        ? t('qa_issue_brackets_extra', normalizedParams)
        : t('qa_issue_brackets_extra_no_params');
    case 'qa_brackets_unclosed':
      return normalizedParams?.bracket
        ? t('qa_issue_brackets_unclosed', normalizedParams)
        : t('qa_issue_brackets_unclosed_no_params');
    case 'qa_brackets_unmatched_close':
      return normalizedParams?.bracket
        ? t('qa_issue_brackets_unmatched_close', normalizedParams)
        : t('qa_issue_brackets_unmatched_close_no_params');
    case 'qa_repeated_word':
      return normalizedParams?.word
        ? t('qa_issue_repeated_word', normalizedParams)
        : t('qa_issue_repeated_word_no_params');
    case 'qa_placeholders_missing':
      return normalizedParams?.placeholder
        ? t('qa_issue_placeholders_missing', normalizedParams)
        : t('qa_issue_placeholders_missing_no_params');
    case 'qa_placeholders_extra':
      return normalizedParams?.placeholder
        ? t('qa_issue_placeholders_extra', normalizedParams)
        : t('qa_issue_placeholders_extra_no_params');
    case 'qa_html_tag_missing':
      return normalizedParams?.tag
        ? t('qa_issue_html_tag_missing', normalizedParams)
        : t('qa_issue_html_tag_missing_no_params');
    case 'qa_html_tag_extra':
      return normalizedParams?.tag
        ? t('qa_issue_html_tag_extra', normalizedParams)
        : t('qa_issue_html_tag_extra_no_params');
    case 'qa_html_unclosed_tag':
      return normalizedParams?.tag
        ? t('qa_issue_html_unclosed_tag', normalizedParams)
        : t('qa_issue_html_unclosed_tag_no_params');
    case 'qa_html_unopened_tag':
      return normalizedParams?.tag
        ? t('qa_issue_html_unopened_tag', normalizedParams)
        : t('qa_issue_html_unopened_tag_no_params');
    case 'qa_icu_syntax_error':
      return t('qa_issue_icu_syntax_error');
    case 'qa_spelling_error':
      return normalizedParams?.word
        ? t('qa_issue_spelling_error', normalizedParams)
        : t('qa_issue_spelling_error_no_params');
    case 'qa_grammar_error':
      return normalizedParams?.message
        ? t('qa_issue_grammar_error', normalizedParams)
        : t('qa_issue_grammar_error_no_params');
    default:
      return message;
  }
}
