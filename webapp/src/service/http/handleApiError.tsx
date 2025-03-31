import { messageService } from 'tg.service/MessageService';
import { T } from '@tolgee/react';
import { GlobalError } from 'tg.error/GlobalError';
import { TranslatedError } from 'tg.translationTools/TranslatedError';
import * as Sentry from '@sentry/browser';
import { parseErrorResponse } from 'tg.fixtures/errorFIxtures';
import { RequestOptions } from './ApiHttpService';
import { globalContext } from 'tg.globalContext/globalActions';
import { LINKS } from 'tg.constants/links';
import { matchPath } from 'react-router-dom';

// Paths which user must be able to access during SSO migration
const SSO_MIGRATION_PATHS = [
  LINKS.SSO_MIGRATION,
  LINKS.ACCEPT_AUTH_PROVIDER_CHANGE,
];

export const handleApiError = (
  r: Response,
  resObject: any,
  init: RequestInit | undefined,
  options: RequestOptions
) => {
  if (r.status >= 500) {
    const message =
      '500: ' + (resObject?.message || 'Error status code from server');
    globalContext.actions?.setGlobalError(new GlobalError(message));
    return;
  }
  if (!options.disableAuthRedirect) {
    if (r.status == 401) {
      // eslint-disable-next-line no-console
      console.warn('Redirecting to login - unauthorized user');
      messageService.error(
        resObject?.code ? (
          <TranslatedError code={resObject?.code} params={resObject?.params} />
        ) : (
          <T keyName="expired_jwt_token" />
        )
      );
      globalContext.actions?.exitDebugCustomerAccountOrLogout();
      return;
    }
    if (r.status == 403) {
      if (resObject?.code === 'email_not_verified') {
        globalContext.actions?.redirectTo(LINKS.ROOT.build());
        return;
      }

      if (resObject?.code === 'sso_login_forced_for_this_account') {
        const currentLocation = globalContext.actions?.currentLocation();
        const alreadyCorrectPath =
          currentLocation &&
          SSO_MIGRATION_PATHS.some((link) =>
            matchPath(currentLocation, {
              path: link.template,
              exact: true,
              strict: false,
            })
          );
        if (alreadyCorrectPath) {
          // Safety net check: Don't redirect if user is already on one of the SSO migration pages
          // This shouldn't happen; It means frontend is trying to load one of the disabled
          // endpoints in the background and it should be fixed
          messageService.error(
            <TranslatedError
              code={resObject?.code}
              params={resObject?.params}
            />
          );
          return;
        }
        globalContext.actions?.redirectTo(LINKS.SSO_MIGRATION.build());
        return;
      }

      if (init?.method === undefined || init?.method === 'get') {
        globalContext.actions?.redirectTo(LINKS.AFTER_LOGIN.build());
      }

      messageService.error(
        resObject?.code ? (
          <TranslatedError code={resObject?.code} params={resObject?.params} />
        ) : (
          <T keyName="operation_not_permitted_error" />
        )
      );
      Sentry.captureException(new Error('Operation not permitted'));
      return;
    }
  }
  if (r.status == 404 && !options.disable404Redirect) {
    if (init?.method === undefined || init?.method === 'get') {
      globalContext.actions?.redirectTo(LINKS.AFTER_LOGIN.build());
    }
    messageService.error(<T keyName="resource_not_found_message" />);
    return;
  }

  if (r.status == 429) {
    messageService.error(<T keyName="too_many_requests" />);
    return;
  }

  if (r.status == 400 && !options.disableErrorNotification) {
    const parsed = parseErrorResponse(resObject);
    parsed.forEach((message) =>
      messageService.error(
        <TranslatedError code={message} params={resObject?.params} />
      )
    );
    return;
  }
};
