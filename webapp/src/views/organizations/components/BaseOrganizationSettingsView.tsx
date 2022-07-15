import { useHistory, useRouteMatch } from 'react-router-dom';

import { BaseViewProps } from 'tg.component/layout/BaseView';
import { Link, LINKS, PARAMS } from 'tg.constants/links';
import { useApiQuery } from 'tg.service/http/useQueryApi';

import { components } from 'tg.service/apiSchema.generated';
import { OrganizationSwitch } from 'tg.component/OrganizationSwitch';
import { NavigationItem } from 'tg.component/navigation/Navigation';
import { useTranslate } from '@tolgee/react';
import { BaseSettingsView } from 'tg.component/layout/BaseSettingsView/BaseSettingsView';
import { SettingsMenuItem } from 'tg.component/layout/BaseSettingsView/SettingsMenu';
import { useConfig, usePreferredOrganization } from 'tg.globalContext/helpers';

type OrganizationModel = components['schemas']['OrganizationModel'];

type Props = BaseViewProps & {
  link: Link;
};

export const BaseOrganizationSettingsView: React.FC<Props> = ({
  children,
  loading,
  navigation,
  link,
  ...otherProps
}) => {
  const config = useConfig();
  const match = useRouteMatch();
  const organizationSlug = match.params[PARAMS.ORGANIZATION_SLUG];
  const t = useTranslate();
  const history = useHistory();
  const { preferredOrganization } = usePreferredOrganization();

  const handleOrganizationSelect = (organization: OrganizationModel) => {
    const redirectLink =
      organization.currentUserRole === 'OWNER'
        ? link
        : LINKS.ORGANIZATION_PROFILE;

    history.push(
      redirectLink.build({ [PARAMS.ORGANIZATION_SLUG]: organization.slug })
    );
  };

  const menuItems: SettingsMenuItem[] = [
    {
      link: LINKS.ORGANIZATION_PROFILE.build({
        [PARAMS.ORGANIZATION_SLUG]: organizationSlug,
      }),
      label: t('organization_menu_profile'),
    },
  ];

  if (preferredOrganization.currentUserRole === 'OWNER') {
    menuItems.push({
      link: LINKS.ORGANIZATION_MEMBERS.build({
        [PARAMS.ORGANIZATION_SLUG]: organizationSlug,
      }),
      label: t('organization_menu_members'),
    });
    menuItems.push({
      link: LINKS.ORGANIZATION_MEMBER_PRIVILEGES.build({
        [PARAMS.ORGANIZATION_SLUG]: organizationSlug,
      }),
      label: t('organization_menu_member_privileges'),
    });
    if (config.billing.enabled) {
      menuItems.push({
        link: LINKS.ORGANIZATION_BILLING.build({
          [PARAMS.ORGANIZATION_SLUG]: organizationSlug,
        }),
        label: t('organization_menu_billing'),
      });
    }
  }

  const organizationLoadable = useApiQuery({
    url: '/v2/organizations/{slug}',
    method: 'get',
    path: { slug: organizationSlug },
  });

  const navigationPrefix: NavigationItem[] = organizationLoadable.data
    ? [[<OrganizationSwitch key={0} onSelect={handleOrganizationSelect} />]]
    : [];

  return (
    <BaseSettingsView
      {...otherProps}
      loading={organizationLoadable.isLoading || loading}
      navigation={[...navigationPrefix, ...(navigation || [])]}
      menuItems={menuItems}
      hideChildrenOnLoading={false}
    >
      {children}
    </BaseSettingsView>
  );
};
