import { BaseViewProps } from 'tg.component/layout/BaseView';
import { LINKS } from 'tg.constants/links';

import { useTranslate } from '@tolgee/react';
import { BaseSettingsView } from 'tg.component/layout/BaseSettingsView/BaseSettingsView';
import { SettingsMenuItem } from 'tg.component/layout/BaseSettingsView/SettingsMenu';
import { useConfig } from 'tg.globalContext/helpers';

type Props = BaseViewProps;

export const BaseUserSettingsView: React.FC<Props> = ({
  children,
  navigation,
  ...otherProps
}) => {
  const t = useTranslate();
  const { authentication } = useConfig();
  const menuItems: SettingsMenuItem[] = authentication
    ? [
        {
          link: LINKS.USER_PROFILE.build(),
          label: t('user_menu_user_settings'),
        },
      ]
    : [];

  menuItems.push({
    link: LINKS.USER_API_KEYS.build(),
    label: t('user_menu_api_keys'),
  });

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
