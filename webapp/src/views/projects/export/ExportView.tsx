import { useTranslate } from '@tolgee/react';
import { FunctionComponent } from 'react';

import { BaseView } from 'tg.component/layout/BaseView';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';
import { ExportForm } from './ExportForm';

export const ExportView: FunctionComponent = () => {
  const project = useProject();
  const t = useTranslate();

  return (
    <BaseView
      navigation={[
        [
          project.name,
          LINKS.PROJECT_TRANSLATIONS.build({
            [PARAMS.PROJECT_ID]: project.id,
          }),
        ],
        [
          t('export_translations_title'),
          LINKS.PROJECT_EXPORT.build({
            [PARAMS.PROJECT_ID]: project.id,
          }),
        ],
      ]}
      lg={7}
      md={9}
      containerMaxWidth="lg"
    >
      <ExportForm />
    </BaseView>
  );
};
