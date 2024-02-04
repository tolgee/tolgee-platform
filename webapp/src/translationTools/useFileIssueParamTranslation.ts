import { useTranslate } from '@tolgee/react';

export const useFileIssuePeramTranslation = () => {
  const { t } = useTranslate();
  return function (type: string, value: string) {
    switch (type) {
      case 'file_node_original':
        return t('import_file_issue_param_type_file_node_original', { value });
      case 'key_index':
        return t('import_file_issue_param_type_key_index', { value });
      case 'key_name':
        return t('import_file_issue_param_type_key_name', { value });
      case 'key_id':
        return t('import_file_issue_param_type_key_id', { value });
      case 'language_id':
        return t('import_file_issue_param_type_language_id', { value });
      case 'language_name':
        return t('import_file_issue_param_type_language_name', { value });
      case 'line':
        return t('import_file_issue_param_type_line', { value });
      case 'value':
        return t('import_file_issue_param_type_value', { value });
      default:
        return type;
    }
  };
};
