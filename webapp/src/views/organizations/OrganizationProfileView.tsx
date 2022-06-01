import { FunctionComponent, useState } from 'react';
import { Box, Button } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { Redirect, useRouteMatch } from 'react-router-dom';
import { container } from 'tsyringe';

import { StandardForm } from 'tg.component/common/form/StandardForm';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { LINKS, PARAMS } from 'tg.constants/links';
import { confirmation } from 'tg.hooks/confirmation';
import { MessageService } from 'tg.service/MessageService';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { RedirectionActions } from 'tg.store/global/RedirectionActions';

import { BaseOrganizationSettingsView } from './BaseOrganizationSettingsView';
import { OrganizationFields } from './components/OrganizationFields';
import { OrganizationProfileAvatar } from './OrganizationProfileAvatar';

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
    <BaseOrganizationSettingsView
      windowTitle={t('edit_organization_title')}
      link={LINKS.ORGANIZATION_PROFILE}
      title={t('edit_organization_title')}
      loading={organization.isFetching || deleteOrganization.isLoading}
      hideChildrenOnLoading={false}
    >
      <Box data-cy="organization-profile">
        <StandardForm
          initialValues={initialValues!}
          saveActionLoadable={editOrganization}
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
            <OrganizationProfileAvatar />
            <OrganizationFields />
          </>
        </StandardForm>
      </Box>
    </BaseOrganizationSettingsView>
  );
};
