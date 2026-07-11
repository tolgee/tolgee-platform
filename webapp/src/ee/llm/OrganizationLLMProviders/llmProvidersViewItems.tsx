import { useTranslate } from '@tolgee/react';
import { LINKS } from 'tg.constants/links';
import { LlmProvidersCustom } from './LlmProvidersCustom';
import { LlmProvidersServer } from './LlmProvidersServer';
import { useRouteMatch } from 'react-router-dom';
import { components } from 'tg.service/apiSchema.generated';

export type ProviderItem = {
  id?: number | undefined;
  name: string;
  type: components['schemas']['LlmProviderModel']['type'];
};

export const useLlmProvidersViewItems = () => {
  const { t } = useTranslate();

  const items = [
    {
      value: 'custom',
      tab: {
        label: t('llm_providers_custom'),
        dataCy: 'llm-providers-custom',
        condition: () => true,
      },
      link: LINKS.ORGANIZATION_LLM_PROVIDERS,
      requireExactMath: true,
      component: LlmProvidersCustom,
    },
    {
      value: 'server',
      tab: {
        label: t('llm_providers_server'),
        dataCy: 'llm-providers-server',
        condition: () => true,
      },
      link: LINKS.ORGANIZATION_LLM_PROVIDERS_SERVER,
      requireExactMath: true,
      component: LlmProvidersServer,
    },
  ] as const;

  const value = items
    .map((item) => {
      const routerMatch = useRouteMatch(item.link.template);
      if (!routerMatch) {
        return [item.value, false];
      }

      return [item.value, !(item.requireExactMath && !routerMatch.isExact)];
    })
    .find((v) => v[1])?.[0] as string | undefined;

  const ActiveComponent = items.find((item) => item.value === value)?.component;

  return {
    items,
    value,
    ActiveComponent,
  };
};
