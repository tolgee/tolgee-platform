import { FunctionComponent } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { container } from 'tsyringe';

import { BaseFormView } from 'tg.component/layout/BaseFormView';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { LINKS } from 'tg.constants/links';
import { MessageService } from 'tg.service/MessageService';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { ProjectPermissionType } from 'tg.service/response.types';
import { RedirectionActions } from 'tg.store/global/RedirectionActions';

import { OrganizationFields } from './components/OrganizationFields';
import { usePreferredOrganization } from 'tg.globalContext/helpers';

type OrganizationBody = components['schemas']['OrganizationDto'];

const redirectionActions = container.resolve(RedirectionActions);
const messageService = container.resolve(MessageService);

export const OrganizationCreateView: FunctionComponent = () => {
  const loadable = useApiMutation({
    url: '/v2/organizations',
    method: 'post',
  });
  const t = useTranslate();
  const { updatePreferredOrganization } = usePreferredOrganization();

  const onSubmit = (values) => {
    loadable.mutate(
      { content: { 'application/json': values } },
      {
        onSuccess: (organization) => {
          updatePreferredOrganization(organization);
          redirectionActions.redirect.dispatch(LINKS.PROJECTS.build());
          messageService.success(<T>organization_created_message</T>);
        },
      }
    );
  };

  const initialValues: OrganizationBody = {
    name: '',
    slug: '',
    description: '',
    basePermissions: ProjectPermissionType.VIEW,
  };

  return (
    <DashboardPage>
      <BaseFormView
        windowTitle={t('create_organization_title')}
        lg={6}
        md={8}
        title={<T>create_organization_title</T>}
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
