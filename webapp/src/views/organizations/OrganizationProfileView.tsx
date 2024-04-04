import { FunctionComponent, useState } from 'react';
import { Box, Button, Typography } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { Redirect, useHistory, useRouteMatch } from 'react-router-dom';

import { DangerZone } from 'tg.component/DangerZone/DangerZone';
import { StandardForm } from 'tg.component/common/form/StandardForm';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { LINKS, PARAMS } from 'tg.constants/links';
import { confirmation } from 'tg.hooks/confirmation';
import { messageService } from 'tg.service/MessageService';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { DangerButton } from 'tg.component/DangerZone/DangerButton';

import { BaseOrganizationSettingsView } from './components/BaseOrganizationSettingsView';
import { OrganizationFields } from './components/OrganizationFields';
import { OrganizationProfileAvatar } from './OrganizationProfileAvatar';
import { useLeaveOrganization } from './useLeaveOrganization';
import { useIsAdmin } from 'tg.globalContext/helpers';

type OrganizationBody = components['schemas']['OrganizationDto'];

export const OrganizationProfileView: FunctionComponent = () => {
  const { t } = useTranslate();
  const leaveOrganization = useLeaveOrganization();
  const { invalidateInitialData } = useGlobalActions();
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
    invalidatePrefix: '/v2/organizations',
  });

  const deleteOrganization = useApiMutation({
    url: '/v2/organizations/{id}',
    method: 'delete',
    invalidatePrefix: '/v2/organizations',
  });

  const isAdmin = useIsAdmin();

  const readOnly = organization.data?.currentUserRole !== 'OWNER' && !isAdmin;
  const notMember = !organization.data?.currentUserRole;

  const onSubmit = (values: OrganizationBody) => {
    const toSave = {
      name: values.name,
      description: values.description,
      slug: values.slug,
    } as OrganizationBody;

    editOrganization.mutate(
      {
        path: { id: organization.data!.id },
        content: { 'application/json': toSave },
      },
      {
        onSuccess: (data) => {
          invalidateInitialData();
          if (data.slug !== organizationSlug) {
            history.replace(
              LINKS.ORGANIZATION_PROFILE.build({
                [PARAMS.ORGANIZATION_SLUG]: data.slug,
              })
            );
          }
          messageService.success(<T keyName="organization_updated_message" />);
        },
      }
    );
  };

  const initialValues = organization.data;
  const [cancelled, setCancelled] = useState(false);

  const handleDelete = () => {
    confirmation({
      hardModeText: organization.data?.name.toUpperCase(),
      message: <T keyName="delete_organization_confirmation_message" />,
      onConfirm: () =>
        deleteOrganization.mutate(
          { path: { id: organization.data!.id } },
          {
            onSuccess: async () => {
              messageService.success(
                <T keyName="organization_deleted_message" />
              );
              history.push(LINKS.PROJECTS.build());
              invalidateInitialData();
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
      navigation={[
        [
          t('edit_organization_title'),
          LINKS.ORGANIZATION_PROFILE.build({
            [PARAMS.ORGANIZATION_SLUG]: organizationSlug,
          }),
        ],
      ]}
      loading={organization.isFetching || deleteOrganization.isLoading}
      hideChildrenOnLoading={false}
      maxWidth="normal"
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
                <T keyName="organization_leave_button" />
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
            <T keyName="project_settings_danger_zone_title" />
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
                  <T keyName="organization_delete_button" />
                </DangerButton>
              ),
            },
          ]}
        />
      </Box>
    </BaseOrganizationSettingsView>
  );
};
