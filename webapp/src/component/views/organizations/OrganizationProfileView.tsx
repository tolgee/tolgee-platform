import { useRouteMatch } from 'react-router-dom';
import { FunctionComponent, useState } from 'react';
import { container } from 'tsyringe';
import { T, useTranslate } from '@tolgee/react';
import { Redirect } from 'react-router-dom';
import { Validation } from '../../../constants/GlobalValidationSchema';
import { OrganizationFields } from './components/OrganizationFields';
import { MessageService } from '../../../service/MessageService';
import { StandardForm } from '../../common/form/StandardForm';
import { BaseOrganizationSettingsView } from './BaseOrganizationSettingsView';
import { Button } from '@material-ui/core';
import { confirmation } from '../../../hooks/confirmation';
import { components } from '../../../service/apiSchema.generated';
import { RedirectionActions } from '../../../store/global/RedirectionActions';
import { LINKS, PARAMS } from '../../../constants/links';
import { useApiMutation, useApiQuery } from '../../../service/http/useQueryApi';

type OrganizationBody = components['schemas']['OrganizationDto'];

const redirectionActions = container.resolve(RedirectionActions);
const messageService = container.resolve(MessageService);

export const OrganizationProfileView: FunctionComponent = () => {
  const t = useTranslate();

  const match = useRouteMatch();
  const organizationSlug = match.params[PARAMS.ORGANIZATION_SLUG];

  const organization = useApiQuery({
    url: '/v2/organizations/{slug}',
    method: 'get',
    path: { slug: organizationSlug },
  });

  const editOrganization = useApiMutation({
    url: '/v2/organizations/{id}',
    method: 'put',
  });
  const deleteOrganization = useApiMutation({
    url: '/v2/organizations/{id}',
    method: 'delete',
  });

  const onSubmit = (values: OrganizationBody) => {
    const toSave = {
      name: values.name,
      description: values.description,
      basePermissions: values.basePermissions,
      slug: values.slug,
    } as OrganizationBody;

    editOrganization.mutate(
      {
        path: { id: organization.data!.id },
        content: { 'application/json': toSave },
      },
      {
        onSuccess: (data) => {
          if (data.slug !== organizationSlug) {
            redirectionActions.redirect.dispatch(
              LINKS.ORGANIZATION_PROFILE.build({
                [PARAMS.ORGANIZATION_SLUG]: data.slug,
              })
            );
          } else {
            organization.refetch();
          }
          messageService.success(<T>organization_updated_message</T>);
        },
      }
    );
  };

  const initialValues = organization.data;
  const [cancelled, setCancelled] = useState(false);

  const handleDelete = () => {
    confirmation({
      hardModeText: organization.data?.name.toUpperCase(),
      message: <T>delete_organization_confirmation_message</T>,
      onConfirm: () =>
        deleteOrganization.mutate(
          { path: { id: organization.data!.id } },
          {
            onSuccess: () => {
              messageService.success(<T>organization_deleted_message</T>);
              redirectionActions.redirect.dispatch(LINKS.ORGANIZATIONS.build());
            },
          }
        ),
    });
  };

  if (cancelled) {
    return <Redirect to={LINKS.ORGANIZATIONS.build()} />;
  }

  return (
    <BaseOrganizationSettingsView title={<T>edit_organization_title</T>}>
      <StandardForm
        initialValues={initialValues!}
        onSubmit={onSubmit}
        onCancel={() => setCancelled(true)}
        validationSchema={Validation.ORGANIZATION_CREATE_OR_EDIT(
          t,
          initialValues?.slug
        )}
        customActions={
          <Button
            data-cy="organization-delete-button"
            color="secondary"
            variant="outlined"
            onClick={handleDelete}
          >
            <T>organization_delete_button</T>
          </Button>
        }
      >
        <>
          <OrganizationFields />
        </>
      </StandardForm>
    </BaseOrganizationSettingsView>
  );
};
