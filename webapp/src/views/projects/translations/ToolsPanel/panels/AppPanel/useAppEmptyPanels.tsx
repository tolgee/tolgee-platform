import { useMemo } from 'react';

import { useApiQuery } from 'tg.service/http/useQueryApi';
import { PanelConfig } from '../../common/types';
import { createAppPanelComponent } from './AppPanel';
import { AppIcon } from '../../../../apps/AppIcon';

/**
 * App panels contributed via the `translation-tools-panel-empty` module — shown
 * in the tools panel when no translation cell is selected. The panel iframe
 * receives the view's selected language tags (see {@link createAppPanelComponent}
 * `empty`) instead of a key/language selection.
 */
export function useAppEmptyPanels(projectId: number): PanelConfig[] {
  const apps = useApiQuery({
    url: '/v2/projects/{projectId}/apps',
    method: 'get',
    path: { projectId },
    options: { staleTime: 60_000 },
  });

  return useMemo(() => {
    const installs = apps.data?._embedded?.projectApps ?? [];
    const result: PanelConfig[] = [];
    for (const install of installs) {
      if (!install.enabled) continue;
      const modules = install.modules?.['translation-tools-panel-empty'] ?? [];
      for (const module of modules) {
        result.push({
          id: `app-empty:${install.id}:${module.key}`,
          name: module.title,
          icon: <AppIcon icon={module.icon} fontSize="1em" />,
          component: createAppPanelComponent({
            installId: install.id,
            baseUrl: install.baseUrl,
            entry: module.entry,
            empty: true,
          }),
          hideCount: true,
        });
      }
    }
    return result;
  }, [apps.data]);
}
