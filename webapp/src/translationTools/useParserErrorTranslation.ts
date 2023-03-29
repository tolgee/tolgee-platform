import { useTranslate } from '@tolgee/react';

export function useParserErrorTranslation() {
  const { t } = useTranslate();

  return function (type: string) {
    switch (type) {
      case 'expect_argument_closing_brace':
        return t('parser_expect_argument_closing_brace');
      case 'empty_argument':
        return t('parser_empty_argument');
      case 'malformed_argument':
        return t('parser_malformed_argument');
      case 'expect_argument_type':
        return t('parser_expect_argument_type');
      case 'invalid_argument_type':
        return t('parser_invalid_argument_type');
      case 'expect_argument_style':
        return t('parser_expect_argument_style');
      case 'invalid_number_skeleton':
        return t('parser_invalid_number_skeleton');
      case 'invalid_date_time_skeleton':
        return t('parser_invalid_date_time_skeleton');
      case 'expect_number_skeleton':
        return t('parser_expect_number_skeleton');
      case 'expect_date_time_skeleton':
        return t('parser_expect_date_time_skeleton');
      case 'unclosed_quote_in_argument_style':
        return t('parser_unclosed_quote_in_argument_style');
      case 'expect_select_argument_options':
        return t('parser_expect_select_argument_options');
      case 'expect_plural_argument_offset_value':
        return t('parser_expect_plural_argument_offset_value');
      case 'invalid_plural_argument_offset_value':
        return t('parser_invalid_plural_argument_offset_value');
      case 'expect_select_argument_selector':
        return t('parser_expect_select_argument_selector');
      case 'expect_plural_argument_selector':
        return t('parser_expect_plural_argument_selector');
      case 'expect_select_argument_selector_fragment':
        return t('parser_expect_select_argument_selector_fragment');
      case 'expect_plural_argument_selector_fragment':
        return t('parser_expect_plural_argument_selector_fragment');
      case 'invalid_plural_argument_selector':
        return t('parser_invalid_plural_argument_selector');
      case 'duplicate_plural_argument_selector':
        return t('parser_duplicate_plural_argument_selector');
      case 'duplicate_select_argument_selector':
        return t('parser_duplicate_select_argument_selector');
      case 'missing_other_clause':
        return t('parser_missing_other_clause');
      case 'invalid_tag':
        return t('parser_invalid_tag');
      case 'invalid_tag_name':
        return t('parser_invalid_tag_name');
      case 'unmatched_closing_tag':
        return t('parser_unmatched_closing_tag');
      case 'unclosed_tag':
        return t('parser_unclosed_tag');
      default:
        return type;
    }
  };
}
