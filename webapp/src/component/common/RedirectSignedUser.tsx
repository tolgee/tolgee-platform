import { useEffect } from 'react';
import { GlobalLoading } from 'tg.component/GlobalLoading';
import {
  useGlobalActions,
  useGlobalContext,
} from 'tg.globalContext/GlobalContext';

export const RedirectSignedUser: React.FC = ({ children }) => {
  const shouldRedirect = useGlobalContext((c) => Boolean(c.auth.allowPrivate));
  const { redirectAfterLogin } = useGlobalActions();

  useEffect(() => {
    if (shouldRedirect) {
      redirectAfterLogin();
    }
  }, [shouldRedirect]);

  if (shouldRedirect) {
    return <GlobalLoading />;
  } else {
    return <>{children}</>;
  }
};
