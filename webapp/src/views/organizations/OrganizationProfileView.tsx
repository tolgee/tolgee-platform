import { FunctionComponent, useState } from 'react';
import { Box, Button, Typography } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { Redirect, useHistory, useRouteMatch } from 'react-router-dom';
import { container } from 'tsyringe';

import { DangerZone } from 'tg.component/DangerZone/DangerZone';
import { StandardForm } from 'tg.component/common/form/StandardForm';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { LINKS, PARAMS } from 'tg.constants/links';
import { confirmation } from 'tg.hooks/confirmation';
import { MessageService } from 'tg.service/MessageService';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { RedirectionActions } from 'tg.store/global/RedirectionActions';
import { useGlobalDispatch } from 'tg.globalContext/GlobalContext';
import { DangerButton } from 'tg.component/DangerZone/DangerButton';

import { BaseOrganizationSettingsView } from './components/BaseOrganizationSettingsView';
import { OrganizationFields } from './components/OrganizationFields';
import { OrganizationProfileAvatar } from './OrganizationProfileAvatar';
import { useLeaveOrganization } from './useLeaveOrganization';

type OrganizationBody = components['schemas']['OrganizationDto'];

const redirectionActions = container.resolve(RedirectionActions);
const messageService = container.resolve(MessageService);

export const OrganizationProfileView: FunctionComponent = () => {
  const t = useTranslate();
  const leaveOrganization = useLeaveOrganization();
  const initialDataDispatch = useGlobalDispatch();
  const history = useHistory();

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

  const readOnly = organization.data?.currentUserRole !== 'OWNER';
  const notMember = !organization.data?.currentUserRole;

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
            onSuccess: async () => {
              messageService.success(<T>organization_deleted_message</T>);
              await initialDataDispatch({
                type: 'REFETCH_INITIAL_DATA',
              });
              history.push(LINKS.PROJECTS.build());
            },
            onError(e) {
              messageService.error(<T>{e.code}</T>);
            },
          }
        ),
    });
  };

  const handleLeave = () => {
    leaveOrganization(organization.data!.id);
  };

  if (cancelled) {
    return <Redirect to={LINKS.PROJECTS.build()} />;
  }

  return (
    <BaseOrganizationSettingsView
      windowTitle={t('edit_organization_title')}
      link={LINKS.ORGANIZATION_PROFILE}
      title={t('edit_organization_title')}
      loading={organization.isFetching || deleteOrganization.isLoading}
      hideChildrenOnLoading={false}
      containerMaxWidth="md"
    >
      <Box data-cy="organization-profile">
        <StandardForm
          disabled={readOnly}
          initialValues={initialValues!}
          saveActionLoadable={editOrganization}
          onSubmit={onSubmit}
          onCancel={() => setCancelled(true)}
          validationSchema={Validation.ORGANIZATION_CREATE_OR_EDIT(
            t,
            initialValues?.slug
          )}
          customActions={
            <Box display="flex" gap={1}>
              <Button
                data-cy="organization-profile-leave-button"
                color="secondary"
                variant="outlined"
                onClick={handleLeave}
                disabled={notMember}
              >
                <T>organization_leave_button</T>
              </Button>
            </Box>
          }
        >
          <>
            <OrganizationProfileAvatar disabled={readOnly} />
            <OrganizationFields disabled={readOnly} />
          </>
        </StandardForm>

        <Box mt={2} mb={1}>
          <Typography variant={'h5'}>
            <T>project_settings_danger_zone_title</T>
          </Typography>
        </Box>
        <DangerZone
          actions={[
            {
              description: (
                <T keyName="this_will_delete_organization_forever" />
              ),
              button: (
                <DangerButton
                  onClick={handleDelete}
                  disabled={readOnly}
                  data-cy="organization-profile-delete-button"
                >
                  <T>organization_delete_button</T>
                </DangerButton>
              ),
            },
          ]}
        />
      </Box>
    </BaseOrganizationSettingsView>
  );
};
