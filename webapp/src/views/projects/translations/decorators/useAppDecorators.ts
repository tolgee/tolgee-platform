import { useMemo } from 'react';
import { useQueries } from 'react-query';

import { useApiQuery } from 'tg.service/http/useQueryApi';
import { tokenService } from 'tg.service/TokenService';
import { components } from 'tg.service/apiSchema.generated';
import {
  substituteUrlTemplate,
  UrlTemplateVars,
} from './substituteUrlTemplate';

type ProjectAppModel = components['schemas']['ProjectAppModel'];
type KeyActionModule = components['schemas']['KeyActionModule'];
type TranslationActionModule = components['schemas']['TranslationActionModule'];

export type DecoratorVisibility = 'always' | 'on-hover';

type ResolvedDecoratorBase = {
  installId: number;
  baseUrl: string;
  actionKey: string;
  icon: string;
  tooltip: string;
  visibility: DecoratorVisibility;
  count?: number;
};

export type ResolvedDecoratorLink = ResolvedDecoratorBase & {
  type: 'link';
  url: string;
};

export type ResolvedDecoratorTab = ResolvedDecoratorBase & {
  type: 'tab';
  tabKey: string;
};

export type ResolvedDecoratorPanel = ResolvedDecoratorBase & {
  type: 'panel';
  panelKey: string;
};

export type ResolvedDecorator =
  | ResolvedDecoratorLink
  | ResolvedDecoratorTab
  | ResolvedDecoratorPanel;

type DecoratorResponseItem = {
  keyId: number;
  languageTag?: string | null;
  actionKey: string;
  url?: string;
  count?: number;
  visibility?: DecoratorVisibility;
};

type DecoratorResponse = {
  items?: DecoratorResponseItem[];
};

type TokenResponse = { token: string };

const TOKEN_CACHE = new Map<string, Promise<string>>();

async function fetchInstallToken(
  apiUrl: string,
  projectId: number,
  installId: number
): Promise<string> {
  const cacheKey = `${projectId}:${installId}`;
  const cached = TOKEN_CACHE.get(cacheKey);
  if (cached) return cached;
  const pending = (async () => {
    const userJwt = tokenService.getToken();
    const headers: Record<string, string> = {};
    if (userJwt) headers.Authorization = `Bearer ${userJwt}`;
    const res = await fetch(
      `${apiUrl}/v2/projects/${projectId}/apps/${installId}/token`,
      {
        method: 'POST',
        headers,
      }
    );
    if (!res.ok) throw new Error(`mint-token failed: ${res.status}`);
    const data = (await res.json()) as TokenResponse;
    return data.token;
  })().catch((err) => {
    TOKEN_CACHE.delete(cacheKey);
    throw err;
  });
  TOKEN_CACHE.set(cacheKey, pending);
  return pending;
}

async function fetchDecorators(
  apiUrl: string,
  projectId: number,
  installId: number,
  decoratorsUrl: string,
  keyIds: number[],
  languageTags: string[]
): Promise<DecoratorResponseItem[]> {
  if (keyIds.length === 0) return [];
  const token = await fetchInstallToken(apiUrl, projectId, installId);
  const res = await fetch(decoratorsUrl, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({ projectId, keyIds, languageTags }),
  });
  if (!res.ok) {
    // eslint-disable-next-line no-console
    console.warn(
      `[tolgee-apps] decorators fetch for install ${installId} failed: ${res.status}`
    );
    return [];
  }
  const data = (await res.json()) as DecoratorResponse;
  return data.items ?? [];
}

export type DecoratorAnchor = 'key' | 'translation';

export type DecoratorLookupVars = UrlTemplateVars;

export type UseAppDecoratorsResult = {
  getDecorators: (
    anchor: DecoratorAnchor,
    vars: DecoratorLookupVars
  ) => ResolvedDecorator[];
  isLoading: boolean;
};

export function useAppDecorators(
  projectId: number,
  visibleKeyIds: number[],
  visibleLanguageTags: string[]
): UseAppDecoratorsResult {
  const apiUrl = import.meta.env.VITE_APP_API_URL ?? window.location.origin;
  const apps = useApiQuery({
    url: '/v2/projects/{projectId}/apps',
    method: 'get',
    path: { projectId },
    options: { staleTime: 60_000 },
  });

  const installs: ProjectAppModel[] = useMemo(
    () =>
      (apps.data?._embedded?.projectApps ?? []).filter(
        (a) => a.enabled && a.decoratorsUrl
      ),
    [apps.data]
  );

  const keyIdsKey = useMemo(
    () => [...visibleKeyIds].sort((a, b) => a - b).join(','),
    [visibleKeyIds]
  );
  const langTagsKey = useMemo(
    () => [...visibleLanguageTags].sort().join(','),
    [visibleLanguageTags]
  );

  const queries = useQueries(
    installs.map((install) => ({
      queryKey: [
        'app-decorators',
        install.id,
        projectId,
        keyIdsKey,
        langTagsKey,
      ] as const,
      queryFn: () =>
        fetchDecorators(
          apiUrl,
          projectId,
          install.id,
          install.decoratorsUrl!,
          visibleKeyIds,
          visibleLanguageTags
        ),
      staleTime: 30_000,
      enabled: visibleKeyIds.length > 0,
    }))
  );

  const dynamicByInstall = useMemo(() => {
    const map = new Map<number, DecoratorResponseItem[]>();
    installs.forEach((install, idx) => {
      map.set(install.id, queries[idx]?.data ?? []);
    });
    return map;
  }, [installs, queries]);

  const isLoading = queries.some((q) => q.isLoading);

  const getDecorators = useMemo(() => {
    const allInstalls = apps.data?._embedded?.projectApps ?? [];
    return (
      anchor: DecoratorAnchor,
      vars: DecoratorLookupVars
    ): ResolvedDecorator[] => {
      const out: ResolvedDecorator[] = [];
      for (const install of allInstalls) {
        if (!install.enabled) continue;
        const moduleList = (
          anchor === 'key'
            ? install.modules?.['key-action']
            : install.modules?.['translation-action']
        ) as (KeyActionModule | TranslationActionModule)[] | undefined;
        if (!moduleList || moduleList.length === 0) continue;
        const dynamic = dynamicByInstall.get(install.id) ?? [];
        for (const action of moduleList) {
          const dynamicMatch = dynamic.find((item) => {
            if (item.actionKey !== action.key) return false;
            if (vars.keyId != null && item.keyId !== vars.keyId) return false;
            if (anchor === 'translation') {
              if (item.languageTag && item.languageTag !== vars.languageTag)
                return false;
            }
            return true;
          });
          if (action.dynamic && !dynamicMatch) continue;

          const visibility: DecoratorVisibility =
            dynamicMatch?.visibility ?? action.visibility ?? 'on-hover';
          const count = dynamicMatch?.count;

          if (action.type === 'link') {
            const template = dynamicMatch?.url ?? action.urlTemplate;
            if (!template) continue;
            out.push({
              installId: install.id,
              baseUrl: install.baseUrl,
              actionKey: action.key,
              type: 'link',
              icon: action.icon,
              tooltip: action.tooltip,
              visibility,
              count,
              url: substituteUrlTemplate(template, vars),
            });
          } else if (anchor === 'key' && action.type === 'tab') {
            const tabKey = (action as KeyActionModule).tabKey;
            if (!tabKey) continue;
            out.push({
              installId: install.id,
              baseUrl: install.baseUrl,
              actionKey: action.key,
              type: 'tab',
              icon: action.icon,
              tooltip: action.tooltip,
              visibility,
              count,
              tabKey,
            });
          } else if (anchor === 'translation' && action.type === 'panel') {
            const panelKey = (action as TranslationActionModule).panelKey;
            if (!panelKey) continue;
            out.push({
              installId: install.id,
              baseUrl: install.baseUrl,
              actionKey: action.key,
              type: 'panel',
              icon: action.icon,
              tooltip: action.tooltip,
              visibility,
              count,
              panelKey,
            });
          }
        }
      }
      return out;
    };
  }, [apps.data, dynamicByInstall]);

  return { getDecorators, isLoading };
}
