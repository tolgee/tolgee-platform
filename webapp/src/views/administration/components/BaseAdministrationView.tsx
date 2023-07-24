import { useTranslate } from '@tolgee/react';

import { BaseViewProps } from 'tg.component/layout/BaseView';
import { LINKS } from 'tg.constants/links';

import { NavigationItem } from 'tg.component/navigation/Navigation';
import { BaseSettingsView } from 'tg.component/layout/BaseSettingsView/BaseSettingsView';
import { SettingsMenuItem } from 'tg.component/layout/BaseSettingsView/SettingsMenu';
import { useConfig } from 'tg.globalContext/helpers';

type Props = BaseViewProps;

export const BaseAdministrationView: React.FC<Props> = ({
  children,
  loading,
  navigation,
  ...otherProps
}) => {
  const { t } = useTranslate();
  const config = useConfig();

  const menuItems: SettingsMenuItem[] = [
    {
      link: LINKS.ADMINISTRATION_ORGANIZATIONS.build(),
      label: t('administration_organizations'),
    },
    {
      link: LINKS.ADMINISTRATION_USERS.build(),
      label: t('administration_users'),
    },
    {
      link: LINKS.ADMINISTRATION_EE_LICENSE.build(),
      label: t('administration_ee_license'),
    },
  ];

  if (config.billing.enabled) {
    menuItems.push({
      link: LINKS.ADMINISTRATION_BILLING_CLOUD_PLANS.build(),
      label: t('administration_cloud_plans'),
    });

    menuItems.push({
      link: LINKS.ADMINISTRATION_BILLING_EE_PLANS.build(),
      label: t('administration_ee_plans'),
    });
  }

  const navigationPrefix: NavigationItem[] = [
    [t('administration_title'), LINKS.ADMINISTRATION_ORGANIZATIONS.build()],
  ];

  return (
    <BaseSettingsView
      {...otherProps}
      navigation={[...navigationPrefix, ...(navigation || [])]}
      menuItems={menuItems}
      hideChildrenOnLoading={false}
    >
      {children}
    </BaseSettingsView>
  );
};
