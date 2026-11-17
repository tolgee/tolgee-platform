import { useEffect, useRef } from 'react';
import { GlobalLoading } from 'tg.component/GlobalLoading';
import {
  useGlobalActions,
  useGlobalContext,
} from 'tg.globalContext/GlobalContext';

export const RedirectSignedUser: React.FC<React.PropsWithChildren<unknown>> = ({
  children,
}) => {
  const shouldRedirect = useGlobalContext((c) => Boolean(c.auth.allowPrivate));
  const { redirectAfterLogin } = useGlobalActions();
  const redirectedRef = useRef(false);

  useEffect(() => {
    // redirectAfterLogin consumes the saved link, so a repeated call lands on the default page
    if (shouldRedirect && !redirectedRef.current) {
      redirectedRef.current = true;
      redirectAfterLogin();
    }
  }, [shouldRedirect]);

  if (shouldRedirect) {
    return <GlobalLoading />;
  } else {
    return <>{children}</>;
  }
};
