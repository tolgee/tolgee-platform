import { useCallback, useEffect, useRef, useState } from 'react';
import { useSelector } from 'react-redux';
import { container } from 'tsyringe';

import { AppState } from 'tg.store/index';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { GlobalActions } from 'tg.store/global/GlobalActions';
import { components } from 'tg.service/apiSchema.generated';
import { InvitationCodeService } from 'tg.service/InvitationCodeService';

type OrganizationModel = components['schemas']['OrganizationModel'];

export const useInitialDataService = () => {
  const actions = container.resolve(GlobalActions);
  const invitationCodeService = container.resolve(InvitationCodeService);

  const [organization, setOrganization] = useState<
    OrganizationModel | undefined
  >(undefined);
  const security = useSelector((state: AppState) => state.global.security);
  const initialData = useApiQuery({
    url: '/v2/public/initial-data',
    method: 'get',
    options: {
      onSuccess(data) {
        // set organization data only if missing
        setOrganization((org) => (org ? org : data.preferredOrganization));
        const invitationCode = invitationCodeService.getCode();
        actions.updateSecurity.dispatch({
          allowPrivate:
            !data?.serverConfiguration?.authentication ||
            Boolean(data.userInfo),
          allowRegistration:
            data.serverConfiguration.allowRegistrations ||
            Boolean(invitationCode), // if user has invitation code, registration is allowed
        });
      },
      refetchOnMount: false,
      cacheTime: Infinity,
      keepPreviousData: true,
    },
  });

  const organizationLoadable = useApiMutation({
    url: '/v2/organizations/{id}',
    method: 'get',
    options: {
      onSuccess(data) {
        setOrganization(data);
      },
    },
  });

  const setPreferredOrganization = useApiMutation({
    url: '/v2/user-preferences/set-preferred-organization/{organizationId}',
    method: 'put',
  });

  const preferredOrganization =
    organization ?? initialData.data?.preferredOrganization;

  const previousPreferred = useRef<number>();

  const updatePreferredOrganization = useCallback(
    (newOrg: number | OrganizationModel) => {
      let organizationId: number | undefined;
      if (typeof newOrg === 'number') {
        if (organization?.id !== newOrg) {
          organizationLoadable.mutate({ path: { id: newOrg } });
          organizationId = newOrg;
        }
      } else if (newOrg) {
        setOrganization(newOrg as OrganizationModel);
        organizationId = newOrg.id;
      }
      if (
        organizationId !== undefined &&
        organizationId !==
          (previousPreferred.current ?? preferredOrganization?.id)
      ) {
        previousPreferred.current = organizationId;
        setPreferredOrganization.mutate({
          path: { organizationId },
        });
      }
    },
    [organization, setOrganization, organizationLoadable]
  );

  const refetchInitialData = () => {
    setOrganization(undefined);
    return initialData.refetch();
  };

  useEffect(() => {
    refetchInitialData();
  }, [security.jwtToken]);

  if (initialData.error) {
    throw new Error(initialData.error.message || initialData.error);
  }

  return {
    data: {
      ...initialData.data!,
      preferredOrganization,
    },
    isFetching: initialData.isFetching || organizationLoadable.isLoading,
    isLoading: initialData.isLoading,

    refetchInitialData,
    updatePreferredOrganization,
  };
};
