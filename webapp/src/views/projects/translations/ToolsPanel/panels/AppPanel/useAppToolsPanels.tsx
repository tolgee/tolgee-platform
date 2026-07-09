import { useMemo } from 'react';

import { useApiQuery } from 'tg.service/http/useQueryApi';
import { PanelConfig } from '../../common/types';
import { createAppPanelComponent } from './AppPanel';
import { AppIcon } from '../../../../apps/AppIcon';

export function useAppToolsPanels(projectId: number): PanelConfig[] {
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
      const modules = install.modules?.['translation-tools-panel'] ?? [];
      for (const module of modules) {
        result.push({
          id: `app:${install.id}:${module.key}`,
          name: module.title,
          // Size is governed by the shared PanelHeader icon slot; keep the emoji
          // fallback relative (1em) so it scales with it too.
          icon: <AppIcon icon={module.icon} fontSize="1em" />,
          component: createAppPanelComponent({
            installId: install.id,
            baseUrl: install.baseUrl,
            entry: module.entry,
          }),
          hideCount: true,
        });
      }
    }
    return result;
  }, [apps.data]);
}
