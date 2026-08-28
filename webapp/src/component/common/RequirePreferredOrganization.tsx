import { FC, ReactNode } from 'react';

import { FullPageLoading } from 'tg.component/common/FullPageLoading';
import { usePreferredOrganizationResolution } from 'tg.globalContext/helpers';

type Props = {
  fallback: ReactNode;
};

export const RequirePreferredOrganization: FC<
  React.PropsWithChildren<Props>
> = (props) => {
  const resolution = usePreferredOrganizationResolution();

  if (resolution.status === 'resolved') {
    return <>{props.children}</>;
  }

  if (resolution.status === 'resolving') {
    return <FullPageLoading />;
  }

  return <>{props.fallback}</>;
};
