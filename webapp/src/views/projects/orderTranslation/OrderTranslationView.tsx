import { useTranslate } from '@tolgee/react';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';

import { BaseProjectView } from '../BaseProjectView';
import { OrderTranslation } from './OrderTranslation';

export const OrderTranslationView = () => {
  const { t } = useTranslate();
  const project = useProject();
  return (
    <BaseProjectView
      windowTitle={t('project_order_translation_title')}
      title={t('project_order_translation_title')}
      navigation={[
        [
          t('project_order_translation_title'),
          LINKS.PROJECT_ORDER_TRANSLATION.build({
            [PARAMS.PROJECT_ID]: project.id,
          }),
        ],
      ]}
      lg={9}
      md={12}
      containerMaxWidth="lg"
      hideChildrenOnLoading={false}
    >
      <OrderTranslation />
    </BaseProjectView>
  );
};
