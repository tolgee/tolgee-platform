import { useTranslate } from '@tolgee/react';

export function useImportWarningTranslation() {
  const { t } = useTranslate();

  return (code: string) => {
    switch (code.toLocaleLowerCase()) {
      case 'import_file_warning_header_namespace_cannot_be_used_when_feature_is_disabled':
        return t(
          'import_file_warning_header_namespace_cannot_be_used_when_feature_is_disabled'
        );
      case 'import_file_warning_message_namespace_cannot_be_used_when_feature_is_disabled':
        return t(
          'import_file_warning_message_namespace_cannot_be_used_when_feature_is_disabled'
        );
      default:
        return code;
    }
  };
}
