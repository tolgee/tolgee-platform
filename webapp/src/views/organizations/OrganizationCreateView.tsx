import { FunctionComponent } from 'react';
import { T, useTranslate } from '@tolgee/react';

import { BaseFormView } from 'tg.component/layout/BaseFormView';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { LINKS } from 'tg.constants/links';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { messageService } from 'tg.service/MessageService';

import { OrganizationFields } from './components/OrganizationFields';
import { useHistory } from 'react-router-dom';

type OrganizationBody = components['schemas']['OrganizationDto'];

export const OrganizationCreateView: FunctionComponent = () => {
  const loadable = useApiMutation({
    url: '/v2/organizations',
    method: 'post',
  });
  const { t } = useTranslate();
  const { updatePreferredOrganization } = usePreferredOrganization();
  const history = useHistory();

  const onSubmit = (values) => {
    loadable.mutate(
      { content: { 'application/json': values } },
      {
        onSuccess: (organization) => {
          updatePreferredOrganization(organization.id);
          history.push(LINKS.PROJECTS.build());
          messageService.success(<T keyName="organization_created_message" />);
        },
      }
    );
  };

  const initialValues: OrganizationBody = {
    name: '',
    slug: '',
    description: '',
  };

  return (
    <DashboardPage>
      <BaseFormView
        windowTitle={t('create_organization_title')}
        maxWidth="narrow"
        title={<T keyName="create_organization_title" />}
        initialValues={initialValues}
        onSubmit={onSubmit}
        saveActionLoadable={loadable}
        validationSchema={Validation.ORGANIZATION_CREATE_OR_EDIT(t, '')}
      >
        <>
          <OrganizationFields />
        </>
      </BaseFormView>
    </DashboardPage>
  );
};
