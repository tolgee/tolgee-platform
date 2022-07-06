import { useCallback, useState } from 'react';
import { container } from 'tsyringe';

import { createProvider } from 'tg.fixtures/createProvider';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { GlobalActions } from 'tg.store/global/GlobalActions';
import { useSelector } from 'react-redux';
import { AppState } from 'tg.store/index';
import { InvitationCodeService } from 'tg.service/InvitationCodeService';
import { useEffect } from 'react';

type OrganizationModel = components['schemas']['OrganizationModel'];
type InitialDataModel = components['schemas']['InitialDataModel'];

type InitialDataContextType = InitialDataModel & {
  currentOrganization?: OrganizationModel;
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

  const updateCurrentOrganization = useCallback(
    (newOrg: number | OrganizationModel) => {
      if (typeof newOrg === 'number') {
        if (organization?.id !== newOrg) {
          organizationLoadable.mutate({ path: { id: newOrg } });
        }
      } else if (newOrg) {
        setOrganization(newOrg as OrganizationModel);
      }
    },
    [organization, setOrganization, organizationLoadable]
  );

  const dispatch = (action: ActionType) => {
    switch (action.type) {
      case 'UPDATE_ORGANIZATION':
        updateCurrentOrganization(action.payload);
        break;
      case 'REFETCH':
        setOrganization(undefined);
        initialData.refetch();
        break;
    }
  };

  const contextData: InitialDataContextType = {
    ...initialData.data!,
    currentOrganization:
      organization || initialData.data?.preferredOrganization,
    isFetching: initialData.isFetching || organizationLoadable.isLoading,
    isLoading: initialData.isLoading,
  };

  useEffect(() => {
    dispatch({ type: 'REFETCH' });
  }, [security.jwtToken]);

  return [contextData, dispatch];
});

export const useCurrentOrganization = () =>
  useInitialDataContext((v) => v.currentOrganization!);

export const useConfig = () =>
  useInitialDataContext((v) => v.serverConfiguration);

export const useUser = () => useInitialDataContext((v) => v.userInfo);
