import { useTranslate } from '@tolgee/react';
import { FunctionComponent } from 'react';

import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';
import { ExportForm } from './ExportForm';
import { BaseProjectView } from '../BaseProjectView';

export const ExportView: FunctionComponent = () => {
  const project = useProject();
  const { t } = useTranslate();

  return (
    <BaseProjectView
      windowTitle={t('export_translations_title')}
      navigation={[
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
    </BaseProjectView>
  );
};
