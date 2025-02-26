import { Redirect } from 'react-router-dom';
import { GlobalLoading } from 'tg.component/GlobalLoading';
import { LINKS } from 'tg.constants/links';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';

export const RedirectWhenSsoMigrationRequired: React.FC = ({ children }) => {
  const shouldRedirect = useGlobalContext(
    (c) =>
      c.initialData.ssoInfo?.force &&
      c.initialData.userInfo?.accountType !== 'MANAGED'
  );

  if (shouldRedirect) {
    return (
      <Redirect to={LINKS.SSO_MIGRATION.build()}>
        <GlobalLoading />
      </Redirect>
    );
  } else {
    return <>{children}</>;
  }
};
