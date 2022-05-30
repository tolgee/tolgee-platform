import { useTranslate } from '@tolgee/react';
import { FunctionComponent } from 'react';

import { SmallProjectAvatar } from 'tg.component/navigation/SmallProjectAvatar';
import { BaseView } from 'tg.component/layout/BaseView';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';
import { ExportForm } from './ExportForm';

export const ExportView: FunctionComponent<React.PropsWithChildren<unknown>> =
  () => {
    const project = useProject();
    const t = useTranslate();

    return (
      <BaseView
        windowTitle={t('export_translations_title')}
        navigation={[
          [
            project.name,
            LINKS.PROJECT_DASHBOARD.build({
              [PARAMS.PROJECT_ID]: project.id,
            }),
            <SmallProjectAvatar key={0} project={project} />,
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
