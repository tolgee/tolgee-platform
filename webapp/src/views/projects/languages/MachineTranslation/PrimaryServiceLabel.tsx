import { useTranslate } from '@tolgee/react';
import { LabelHint } from 'tg.component/common/LabelHint';

export const PrimaryServiceLabel = () => {
  const { t } = useTranslate();
  return (
    <LabelHint title={t('project_languages_primary_provider_hint')}>
      {t('project_languages_primary_provider', 'Primary')}
    </LabelHint>
  );
};
