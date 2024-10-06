import { PostHog } from 'posthog-js';
import { useEffect } from 'react';
import { getUtmParams } from 'tg.fixtures/utmCookie';
import { useConfig, useUser } from 'tg.globalContext/helpers';

const POSTHOG_INSTANCE_WINDOW_PROPERTY = 'posthogInstance';

const IGNORED_USER_DOMAINS = ['tolgee.io'];

async function loadPosthog() {
  return (await import('posthog-js')).default;
}

export function usePosthog() {
  const userData = useUser();
  const config = useConfig();

  useEffect(() => {
    if (
      config?.postHogApiKey &&
      userData?.id !== undefined &&
      IGNORED_USER_DOMAINS.every(
        (domain) => !userData.username.endsWith(domain)
      )
    ) {
      let cancelled = false;
      const postHogAPIKey = config?.postHogApiKey;
      if (postHogAPIKey) {
        Promise.resolve(
          window[POSTHOG_INSTANCE_WINDOW_PROPERTY] || loadPosthog()
        ).then((posthog: PostHog) => {
          window[POSTHOG_INSTANCE_WINDOW_PROPERTY] = posthog;
          if (!cancelled) {
            posthog.init(postHogAPIKey, {
              api_host: config?.postHogHost || undefined,
              disable_session_recording: true,
            });
            posthog.identify(userData.id.toString(), {
              name: userData.username,
              email: userData.username,
              ...getUtmParams(),
            });
          }
        });
      }

      return () => {
        cancelled = true;
        const ph = window[POSTHOG_INSTANCE_WINDOW_PROPERTY] as
          | PostHog
          | undefined;
        ph?.reset();
      };
    }
  }, [userData?.id, config?.postHogApiKey]);
}
