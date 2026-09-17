import { FC } from 'react';
import { Redirect } from 'react-router-dom';
import {
  useIsEmailVerified,
  usePreferredOrganization,
} from 'tg.globalContext/helpers';
import { LINKS } from 'tg.constants/links';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';

export const RequirePreferredOrganization: FC<
  React.PropsWithChildren<unknown>
> = (props) => {
  const allowPrivate = useGlobalContext((c) => c.auth.allowPrivate);
  const { preferredOrganization, isFetching } = usePreferredOrganization();
  const isEmailVerified = useIsEmailVerified();

  if (!isEmailVerified || !allowPrivate || preferredOrganization) {
    return <>{props.children}</>;
  }

  if (isFetching) {
    return null;
  }

  return <Redirect to={LINKS.COMMUNITY_PROJECTS.build()} />;
};
