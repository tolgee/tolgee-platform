import { useApiQuery } from 'tg.service/http/useQueryApi';
import { tokenService } from 'tg.service/TokenService';

const REFRESH_MARGIN_MS = 60_000;
const MIN_REFRESH_INTERVAL_MS = 5_000;

export function useAppToken(
  projectId: number,
  installId: number
): string | null {
  const tokenLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/apps/{installId}/token',
    method: 'post',
    path: { projectId, installId },
    options: {
      // The token authorizes API calls as the user it was minted for. Dropping it as soon
      // as nothing renders it keeps it from being handed to whoever signs in next in this tab.
      cacheTime: 0,
      refetchInterval: (data) => getRefreshDelay(data?.token),
      refetchIntervalInBackground: true,
    },
  });

  return tokenLoadable.data?.token ?? null;
}

/**
 * App tokens expire (`tolgee.apps.token-expiration`) while the iframe stays open, so the
 * next mint is scheduled off the token's own `exp` rather than a hardcoded lifetime.
 */
function getRefreshDelay(token: string | undefined): number | false {
  if (!token) {
    return false;
  }
  let expiration: number | undefined;
  try {
    expiration = tokenService.parseJwt(token).exp;
  } catch (e) {
    return false;
  }
  if (!expiration) {
    return false;
  }
  return Math.max(
    expiration * 1000 - Date.now() - REFRESH_MARGIN_MS,
    MIN_REFRESH_INTERVAL_MS
  );
}
