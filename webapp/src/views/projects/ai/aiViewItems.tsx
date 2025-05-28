import { Link, LINKS } from 'tg.constants/links';
import { FC, ReactNode } from 'react';
import { useTranslate } from '@tolgee/react';
import { useRouteMatch } from 'react-router-dom';

import { getPermissionTools } from 'tg.fixtures/getPermissionTools';
import { AiContextData, AiPromptsList } from 'tg.ee';

export const useAiViewItems = () => {
  const { t } = useTranslate();

  const items: AiViewItem[] = [
    {
      value: 'prompts',
      tab: {
        label: t('ai_menu_prompts'),
        dataCy: 'ai-menu-prompts',
        condition: () => true,
      },
      link: LINKS.PROJECT_AI,
      requireExactMath: true,
      component: AiPromptsList,
    },
    {
      value: 'context-data',
      tab: {
        label: t('ai_menu_context_data'),
        dataCy: 'ai-menu-context-data',
        condition: () => true,
      },
      link: LINKS.PROJECT_CONTEXT_DATA,
      requireExactMath: true,
      component: AiContextData,
    },
  ];

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

export type AiViewItem = {
  value: string;
  tab: {
    label: ReactNode;
    dataCy: string;
    condition: ConditionType;
  };
  link: Link;
  requireExactMath?: boolean;
  component: FC;
};

type SatisfiedPermissionType = ReturnType<
  typeof getPermissionTools
>['satisfiesPermission'];

type ConditionType = (props: {
  satisfiesPermission: SatisfiedPermissionType;
}) => boolean;
