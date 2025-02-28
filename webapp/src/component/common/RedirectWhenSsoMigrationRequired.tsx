import { Redirect } from 'react-router-dom';
import { GlobalLoading } from 'tg.component/GlobalLoading';
import { LINKS } from 'tg.constants/links';
import { useIsSsoMigrationRequired } from 'tg.globalContext/helpers';

export const RedirectWhenSsoMigrationRequired: React.FC = ({ children }) => {
  const shouldRedirect = useIsSsoMigrationRequired();

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
