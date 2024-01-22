import { useTranslate } from '@tolgee/react';
import { LabelHint } from 'tg.component/common/LabelHint';

export const SuggestionsLabel = () => {
  const { t } = useTranslate();
  return (
    <LabelHint title={t('project_mt_dialog_service_suggested_hint')}>
      {t('project_mt_dialog_service_suggested')}
    </LabelHint>
  );
};
