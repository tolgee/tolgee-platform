import React, { createContext, useCallback, useContext, useState } from 'react';

import { FullPageLoading } from 'tg.component/common/FullPageLoading';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { useUser } from './useUser';

type OrganizationModel = components['schemas']['OrganizationModel'];

export const CurrentOrganizationContext = createContext<OrganizationModel>(
  undefined as unknown as OrganizationModel
);

export const UpdateCurrentOrganizationContext = createContext<
  (id?: number | OrganizationModel) => void
>(() => {});

export const CurrentOrganizationProvider: React.FC = ({ children }) => {
  const user = useUser();
  const [organization, setOrganization] = useState<
    OrganizationModel | undefined
  >(undefined);

  const { refetch } = useApiQuery({
    url: '/v2/preferred-organization',
    method: 'get',
    options: {
      onSuccess(data) {
        // set organization data only if missing
        setOrganization((org) => (org ? org : data));
      },
      enabled: Boolean(!organization && user),
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
    (newOrg?: number | OrganizationModel) => {
      if (newOrg === undefined) {
        setOrganization(undefined);
        refetch();
      } else if (typeof newOrg === 'number') {
        if (organization?.id !== newOrg && user) {
          organizationLoadable.mutate({ path: { id: newOrg } });
        }
      } else if (newOrg) {
        setOrganization(newOrg as OrganizationModel);
      }
    },
    [organization, setOrganization, organizationLoadable]
  );

  if (!organization && user) {
    return <FullPageLoading />;
  }

  return (
    <CurrentOrganizationContext.Provider value={organization!}>
      <UpdateCurrentOrganizationContext.Provider
        value={updateCurrentOrganization}
      >
        {children}
      </UpdateCurrentOrganizationContext.Provider>
    </CurrentOrganizationContext.Provider>
  );
};

export const useUpdateCurrentOrganization = () =>
  useContext(UpdateCurrentOrganizationContext);

export const useCurrentOrganization = () =>
  useContext(CurrentOrganizationContext);
