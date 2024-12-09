import { Link, LINKS } from 'tg.constants/links';
import { FC, ReactNode } from 'react';
import { useTranslate } from '@tolgee/react';
import { useRouteMatch } from 'react-router-dom';

import { getPermissionTools } from 'tg.fixtures/getPermissionTools';
import { createAdder } from 'tg.fixtures/pluginAdder';
import { useAddDeveloperViewItems } from 'tg.ee';

import { CdList } from './contentDelivery/CdList';

export const addDeveloperViewItems = createAdder<DeveloperViewItem>({
  referencingProperty: 'value',
});

export type DeveloperViewItemsAdder = ReturnType<typeof addDeveloperViewItems>;

export const useDeveloperViewItems = () => {
  const { t } = useTranslate();

  const baseItems: DeveloperViewItem[] = [
    {
      value: 'content-delivery',
      tab: {
        label: t('developer_menu_content_delivery'),
        dataCy: 'developer-menu-content-delivery',
        condition: ({ satisfiesPermission }) =>
          satisfiesPermission('content-delivery.publish'),
      },
      link: LINKS.PROJECT_CONTENT_STORAGE,
      requireExactMath: true,
      component: CdList,
    },
  ];

  const addDeveloperViewItems = useAddDeveloperViewItems();

  const items = addDeveloperViewItems(baseItems);

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

export type DeveloperViewItem = {
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
