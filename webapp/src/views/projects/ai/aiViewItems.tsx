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
      value: 'context-data',
      tab: {
        label: t('ai_menu_context_data'),
        dataCy: 'ai-menu-context-data',
        condition: () => true,
      },
      link: LINKS.PROJECT_CONTEXT_DATA,
      requireExactMatch: true,
      component: AiContextData,
    },
    {
      value: 'prompts',
      tab: {
        label: t('ai_menu_prompts'),
        dataCy: 'ai-menu-prompts',
        condition: () => true,
      },
      link: LINKS.PROJECT_AI_PROMPTS,
      requireExactMatch: true,
      component: AiPromptsList,
    },
  ];

  const match = useRouteMatch(items.map((item) => item.link.template));

  const activeItem = match
    ? items.find(
        (item) =>
          item.link.template === match.path &&
          (!item.requireExactMatch || match.isExact)
      )
    : undefined;

  return {
    items,
    value: activeItem?.value,
    ActiveComponent: activeItem?.component,
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
  requireExactMatch?: boolean;
  component: FC;
};

type SatisfiedPermissionType = ReturnType<
  typeof getPermissionTools
>['satisfiesPermission'];

type ConditionType = (props: {
  satisfiesPermission: SatisfiedPermissionType;
}) => boolean;
