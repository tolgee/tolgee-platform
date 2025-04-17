import { useHistory, useRouteMatch } from 'react-router-dom';

import { BaseViewProps } from 'tg.component/layout/BaseView';
import { Link, LINKS, PARAMS } from 'tg.constants/links';
import { useApiQuery } from 'tg.service/http/useQueryApi';

import { components } from 'tg.service/apiSchema.generated';
import { OrganizationSwitch } from 'tg.component/organizationSwitch/OrganizationSwitch';
import { NavigationItem } from 'tg.component/navigation/Navigation';
import { useTranslate } from '@tolgee/react';
import { BaseSettingsView } from 'tg.component/layout/BaseSettingsView/BaseSettingsView';
import { SettingsMenuItem } from 'tg.component/layout/BaseSettingsView/SettingsMenu';
import {
  useConfig,
  useIsAdmin,
  usePreferredOrganization,
} from 'tg.globalContext/helpers';
import { Usage } from 'tg.ee';

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
  const { t } = useTranslate();
  const history = useHistory();
  const { preferredOrganization } = usePreferredOrganization();
  const isAdmin = useIsAdmin();

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

  if (preferredOrganization?.currentUserRole === 'OWNER' || isAdmin) {
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
    menuItems.push({
      link: LINKS.ORGANIZATION_APPS.build({
        [PARAMS.ORGANIZATION_SLUG]: organizationSlug,
      }),
      label: t('organization_menu_apps'),
    });
    if (config.llm.enabled) {
      menuItems.push({
        link: LINKS.ORGANIZATION_LLM_PROVIDERS.build({
          [PARAMS.ORGANIZATION_SLUG]: organizationSlug,
        }),
        label: t('organization_menu_llm_providers'),
      });
    }
    menuItems.push({
      link: LINKS.ORGANIZATION_SSO.build({
        [PARAMS.ORGANIZATION_SLUG]: organizationSlug,
      }),
      label: t('organization_menu_sso_login'),
    });
    if (config.billing.enabled) {
      menuItems.push({
        link: LINKS.ORGANIZATION_SUBSCRIPTIONS.build({
          [PARAMS.ORGANIZATION_SLUG]: organizationSlug,
        }),
        label: t('organization_menu_subscriptions'),
      });
      menuItems.push({
        link: LINKS.ORGANIZATION_INVOICES.build({
          [PARAMS.ORGANIZATION_SLUG]: organizationSlug,
        }),
        label: t('organization_menu_invoices'),
      });
      if (config.internalControllerEnabled) {
        menuItems.push({
          link: LINKS.ORGANIZATION_BILLING_TEST_CLOCK_HELPER.build({
            [PARAMS.ORGANIZATION_SLUG]: organizationSlug,
          }),
          label: t('organization-menu-billing-test-clock'),
        });
      }
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
      navigationRight={<Usage />}
      menuItems={menuItems}
      hideChildrenOnLoading={false}
    >
      {children}
    </BaseSettingsView>
  );
};
