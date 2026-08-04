import { useEffect, useState } from 'react';

import { useApiMutation } from 'tg.service/http/useQueryApi';

const tokenCache = new Map<string, string>();

export function useAppToken(
  projectId: number,
  installId: number
): string | null {
  const cacheKey = `${projectId}:${installId}`;
  const [token, setToken] = useState<string | null>(
    () => tokenCache.get(cacheKey) ?? null
  );

  const mutation = useApiMutation({
    url: '/v2/projects/{projectId}/apps/{installId}/token',
    method: 'post',
  });

  useEffect(() => {
    if (tokenCache.has(cacheKey)) return;
    let cancelled = false;
    mutation.mutate(
      { path: { projectId, installId } },
      {
        onSuccess: (data) => {
          if (cancelled) return;
          tokenCache.set(cacheKey, data.token);
          setToken(data.token);
        },
      }
    );
    return () => {
      cancelled = true;
    };
  }, [cacheKey]);

  return token;
}
