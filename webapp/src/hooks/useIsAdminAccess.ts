import { useMemo } from 'react';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';
import { components } from 'tg.service/apiSchema.generated';

type PermissionModel = components['schemas']['ComputedPermissionModel'];

export function useIsAdminAccess(
  computedPermission: PermissionModel | undefined
) {
  const billingEnabled = useGlobalContext(
    (c) => c.initialData.serverConfiguration.billing.enabled
  );

  return useMemo(
    () =>
      billingEnabled &&
      (computedPermission?.origin === 'SERVER_ADMIN' ||
        computedPermission?.origin === 'SERVER_SUPPORTER'),
    [billingEnabled, computedPermission?.origin]
  );
}
