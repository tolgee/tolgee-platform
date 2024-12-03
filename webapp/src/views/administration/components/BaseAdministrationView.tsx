import { useTranslate } from '@tolgee/react';

import { BaseViewProps } from 'tg.component/layout/BaseView';
import { Link, LINKS } from 'tg.constants/links';

import { NavigationItem } from 'tg.component/navigation/Navigation';
import { BaseSettingsView } from 'tg.component/layout/BaseSettingsView/BaseSettingsView';
import { createAdder } from 'tg.fixtures/pluginAdder';
import { getEe } from '../../../plugin/getEe';

type Props = BaseViewProps;

export const BaseAdministrationView: React.FC<Props> = ({
  children,
  loading,
  navigation,
  ...otherProps
}) => {
  const { t } = useTranslate();

  const { useAddAdministrationMenuItems } = getEe();
  const addItems = useAddAdministrationMenuItems();

  const baseItems: AdministrationMenuItem[] = [
    {
      id: 'organizations',
      link: LINKS.ADMINISTRATION_ORGANIZATIONS,
      label: t('administration_organizations'),
      condition: () => true,
    },
    {
      id: 'users',
      link: LINKS.ADMINISTRATION_USERS,
      label: t('administration_users'),
      condition: () => true,
    },
  ];

  const menuItems = addItems(baseItems);

  const navigationPrefix: NavigationItem[] = [
    [t('administration_title'), LINKS.ADMINISTRATION_ORGANIZATIONS.build()],
  ];

  return (
    <BaseSettingsView
      {...otherProps}
      navigation={[...navigationPrefix, ...(navigation || [])]}
      menuItems={menuItems.map((item) => ({
        label: item.label,
        link: item.link.build(),
      }))}
      hideChildrenOnLoading={false}
      maxWidth="normal"
    >
      {children}
    </BaseSettingsView>
  );
};

export type AdministrationMenuItem = {
  link: Link;
  label: string;
  id: string;
  condition: () => boolean;
};

export const addAdministrationMenuItems = createAdder<AdministrationMenuItem>({
  referencingProperty: 'id',
});

export type AdministrationMenuItemsAdder = ReturnType<
  typeof addAdministrationMenuItems
>;
