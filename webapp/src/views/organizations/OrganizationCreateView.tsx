import { FunctionComponent } from 'react';
import { container } from 'tsyringe';
import { T, useTranslate } from '@tolgee/react';
import { DashboardPage } from '../../component/layout/DashboardPage';
import { RedirectionActions } from '../../store/global/RedirectionActions';
import { LINKS } from '../../constants/links';
import { ProjectPermissionType } from '../../service/response.types';
import { BaseFormView } from '../../component/layout/BaseFormView';
import { Validation } from '../../constants/GlobalValidationSchema';
import { OrganizationFields } from './components/OrganizationFields';
import { MessageService } from '../../service/MessageService';
import { components } from '../../service/apiSchema.generated';
import { useApiMutation } from '../../service/http/useQueryApi';

type OrganizationBody = components['schemas']['OrganizationDto'];

const redirectionActions = container.resolve(RedirectionActions);
const messageService = container.resolve(MessageService);

export const OrganizationCreateView: FunctionComponent = () => {
  const loadable = useApiMutation({
    url: '/v2/organizations',
    method: 'post',
  });
  const t = useTranslate();

  const onSubmit = (values) => {
    loadable.mutate(
      { content: { 'application/json': values } },
      {
        onSuccess: () => {
          redirectionActions.redirect.dispatch(LINKS.ORGANIZATIONS.build());
          messageService.success(<T>organization_created_message</T>);
        },
      }
    );
  };

  const onCancel = () => {
    redirectionActions.redirect.dispatch(LINKS.ORGANIZATIONS.build());
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
        lg={6}
        md={8}
        title={<T>create_organization_title</T>}
        initialValues={initialValues}
        onSubmit={onSubmit}
        onCancel={onCancel}
        loading={loadable.isLoading}
        validationSchema={Validation.ORGANIZATION_CREATE_OR_EDIT(t, '')}
      >
        <>
          <OrganizationFields />
        </>
      </BaseFormView>
    </DashboardPage>
  );
};
