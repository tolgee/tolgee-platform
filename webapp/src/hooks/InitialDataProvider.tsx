import { useCallback, useState, useEffect } from 'react';
import { container } from 'tsyringe';
import { useSelector } from 'react-redux';

import { createProvider } from 'tg.fixtures/createProvider';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { GlobalActions } from 'tg.store/global/GlobalActions';
import { AppState } from 'tg.store/index';
import { InvitationCodeService } from 'tg.service/InvitationCodeService';
import { useRef } from 'react';

type OrganizationModel = components['schemas']['OrganizationModel'];
type InitialDataModel = components['schemas']['InitialDataModel'];

type InitialDataContextType = InitialDataModel & {
  preferredOrganization?: OrganizationModel;
  isFetching?: boolean;
  isLoading?: boolean;
};

const actions = container.resolve(GlobalActions);
const invitationCodeService = container.resolve(InvitationCodeService);

type ActionType =
  | {
      type: 'UPDATE_ORGANIZATION';
      payload: number | OrganizationModel;
    }
  | { type: 'REFETCH' };

export const [
  InitialDataProvider,
  useInitialDataDispatch,
  useInitialDataContext,
] = createProvider(() => {
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

  const dispatch = async (action: ActionType) => {
    switch (action.type) {
      case 'UPDATE_ORGANIZATION':
        return updatePreferredOrganization(action.payload);
      case 'REFETCH':
        setOrganization(undefined);
        return initialData.refetch();
    }
  };

  const contextData: InitialDataContextType = {
    ...initialData.data!,
    preferredOrganization: preferredOrganization,
    isFetching: initialData.isFetching || organizationLoadable.isLoading,
    isLoading: initialData.isLoading,
  };

  useEffect(() => {
    dispatch({ type: 'REFETCH' });
  }, [security.jwtToken]);

  if (initialData.error) {
    throw new Error(initialData.error.message || initialData.error);
  }

  return [contextData, dispatch];
});

export const useConfig = () =>
  useInitialDataContext((v) => v.serverConfiguration);

export const useUser = () => useInitialDataContext((v) => v.userInfo);

export const usePreferredOrganization = () => {
  const initialDataDispatch = useInitialDataDispatch();
  const preferredOrganization = useInitialDataContext(
    (v) => v.preferredOrganization!
  );
  const updatePreferredOrganization = (org: number | OrganizationModel) =>
    initialDataDispatch({ type: 'UPDATE_ORGANIZATION', payload: org });
  return { preferredOrganization, updatePreferredOrganization };
};
