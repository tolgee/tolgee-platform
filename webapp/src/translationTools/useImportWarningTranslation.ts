import { useTranslate } from '@tolgee/react';

export function useImportWarningTranslation() {
  const { t } = useTranslate();

  return (code: string) => {
    switch (code.toLocaleLowerCase()) {
      case 'namespace_cannot_be_used_when_feature_is_disabled':
        return {
          title: t(
            'warning_header_namespace_cannot_be_used_when_feature_is_disabled'
          ),
          message: t(
            'warning_message_namespace_cannot_be_used_when_feature_is_disabled'
          ),
        };
      default:
        return {
          title: `warning_header_${code}`,
          message: `warning_message_${code}`,
        };
    }
  };
}
