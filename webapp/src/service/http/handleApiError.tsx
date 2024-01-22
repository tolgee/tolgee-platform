import { messageService } from 'tg.service/MessageService';
import { tokenService } from 'tg.service/TokenService';
import { redirectionActions } from 'tg.store/global/RedirectionActions';
import { T } from '@tolgee/react';
import { LINKS } from 'tg.constants/links';
import { GlobalError } from 'tg.error/GlobalError';
import { errorActions } from 'tg.store/global/ErrorActions';
import { TranslatedError } from 'tg.translationTools/TranslatedError';
import * as Sentry from '@sentry/browser';
import { parseErrorResponse } from 'tg.fixtures/errorFIxtures';
import { RequestOptions } from './ApiHttpService';

export const handleApiError = (
  r: Response,
  resObject: any,
  init: RequestInit | undefined,
  options: RequestOptions
) => {
  if (r.status >= 500) {
    const message =
      '500: ' + (resObject?.message || 'Error status code from server');
    errorActions.globalError.dispatch(new GlobalError(message));
    return;
  }
  if (!options.disableAuthRedirect) {
    if (r.status == 401) {
      // eslint-disable-next-line no-console
      console.warn('Redirecting to login - unauthorized user');
      messageService.error(<T keyName="expired_jwt_token" />);
      redirectionActions.redirect.dispatch(LINKS.LOGIN.build());
      tokenService.disposeToken();
      location.reload();
      return;
    }
    if (r.status == 403) {
      if (init?.method === undefined || init?.method === 'get') {
        redirectionActions.redirect.dispatch(LINKS.AFTER_LOGIN.build());
      }
      messageService.error(<T keyName="operation_not_permitted_error" />);
      Sentry.captureException(new Error('Operation not permitted'));
      return;
    }
  }
  if (r.status == 404 && !options.disable404Redirect) {
    if (init?.method === undefined || init?.method === 'get') {
      redirectionActions.redirect.dispatch(LINKS.AFTER_LOGIN.build());
    }
    messageService.error(<T keyName="resource_not_found_message" />);
    return;
  }
  if (r.status == 400 && !options.disableErrorNotification) {
    const parsed = parseErrorResponse(resObject);
    parsed.forEach((message) =>
      messageService.error(<TranslatedError code={message} />)
    );
    return;
  }
};
