import { BaseViewProps } from 'tg.component/layout/BaseView';
import { LINKS } from 'tg.constants/links';

import { useTranslate } from '@tolgee/react';
import { BaseSettingsView } from 'tg.component/layout/BaseSettingsView/BaseSettingsView';
import { SettingsMenuItem } from 'tg.component/layout/BaseSettingsView/SettingsMenu';
import { useConfig, useIsEmailVerified } from 'tg.globalContext/helpers';

type Props = BaseViewProps;

export const BaseUserSettingsView: React.FC<Props> = ({
  children,
  navigation,
  ...otherProps
}) => {
  const { t } = useTranslate();
  const { authentication } = useConfig();
  const isEmailVerified = useIsEmailVerified();

  const menuItems: SettingsMenuItem[] = authentication
    ? [
        {
          link: LINKS.USER_PROFILE.build(),
          label: t('user_profile_title'),
        },
        {
          link: LINKS.USER_ACCOUNT_SECURITY.build(),
          label: t('user-account-security-title'),
        },
        {
          link: LINKS.USER_ACCOUNT_NOTIFICATIONS.build(),
          label: t('user_menu_notifications'),
        },
      ]
    : [];

  if (isEmailVerified) {
    menuItems.push({
      link: LINKS.USER_API_KEYS.build(),
      label: t('user_menu_api_keys'),
    });

    menuItems.push({
      link: LINKS.USER_PATS.build(),
      label: t('user_menu_pats'),
    });
  }

  return (
    <BaseSettingsView
      {...otherProps}
      navigation={[
        [t('user_settings'), LINKS.USER_PROFILE.build()],
        ...(navigation || []),
      ]}
      menuItems={menuItems}
    >
      {children}
    </BaseSettingsView>
  );
};
