import { useTranslate } from '@tolgee/react';

export const useFileIssueTranslation = () => {
  const { t } = useTranslate();
  return function (type: string) {
    switch (type) {
      case 'id_attribute_not_provided':
        return t('file_issue_type_id_attribute_not_provided');
      case 'key_is_empty':
        return t('file_issue_type_key_is_empty');
      case 'key_is_not_string':
        return t('file_issue_type_key_is_not_string');
      case 'po_msgctxt_not_supported':
        return t('file_issue_type_po_msgctxt_not_supported');
      case 'target_not_provided':
        return t('file_issue_type_target_not_provided');
      case 'translation_too_long':
        return t('file_issue_type_translation_too_long');
      case 'value_is_empty':
        return t('file_issue_type_value_is_empty');
      case 'value_is_not_string':
        return t('file_issue_type_value_is_not_string');
      case 'translation_defined_in_another_file':
        return t('translation_defined_in_another_file');
      case 'key_is_blank':
        return t('key_is_blank');
      case 'multiple_values_for_key_and_language':
        return t('multiple_values_for_key_and_language');
      case 'description_too_long':
        return t('file_issue_type_description_too_long');
      default:
        return type;
    }
  };
};
