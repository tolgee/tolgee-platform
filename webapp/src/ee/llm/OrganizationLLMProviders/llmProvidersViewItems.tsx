import { useTranslate } from '@tolgee/react';
import { LINKS } from 'tg.constants/links';
import { LLMProvidersCustom } from './LLMProvidersCustom';
import { LLMProvidersServer } from './LLMProvidersServer';
import { useRouteMatch } from 'react-router-dom';
import { components } from 'tg.service/apiSchema.generated';

export type ProviderItem = {
  id?: number | undefined;
  name: string;
  type: components['schemas']['LLMProviderModel']['type'];
};

export const useLLMProvidersViewItems = () => {
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
      component: LLMProvidersCustom,
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
      component: LLMProvidersServer,
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
