import { useTranslate } from '@tolgee/react';

export function useQaIssueMessage(message: string): string {
  const { t } = useTranslate();
  switch (message) {
    case 'qa_empty_translation':
      return t('qa_issue_empty_translation');
    case 'qa_check_failed':
      return t('qa_check_failed');
    default:
      return message;
  }
}
