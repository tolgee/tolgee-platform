import { useHistory, useRouteMatch } from 'react-router-dom';

import { BaseViewProps } from 'tg.component/layout/BaseView';
import { Link, LINKS, PARAMS } from 'tg.constants/links';

import { components } from 'tg.service/apiSchema.generated';
import { OrganizationSwitch } from 'tg.component/organizationSwitch/OrganizationSwitch';
import { NavigationItem } from 'tg.component/navigation/Navigation';
import { useTranslate } from '@tolgee/react';
import { BaseSettingsView } from 'tg.component/layout/BaseSettingsView/BaseSettingsView';
import { SettingsMenuItem } from 'tg.component/layout/BaseSettingsView/SettingsMenu';
import { useConfig, useIsAdminOrSupporter } from 'tg.globalContext/helpers';
import { useOrganizationLoadable } from 'tg.views/organizations/useOrganization';
import {
  OrganizationMenuItemId,
  organizationSettingsMenu,
} from 'tg.fixtures/organizationSettingsMenu';
import {
  canViewOrganization,
  canManageOrganization,
  isOwnerOrgRole,
} from 'tg.fixtures/organizationRole';
import { CriticalUsageCircle } from 'tg.ee';

type OrganizationModel = components['schemas']['OrganizationModel'];

type Props = BaseViewProps & {
  link: Link;
};

export const BaseOrganizationSettingsView: React.FC<
  React.PropsWithChildren<Props>
> = ({ children, loading, navigation, link, ...otherProps }) => {
  const config = useConfig();
  const match = useRouteMatch();
  const organizationSlug = match.params[PARAMS.ORGANIZATION_SLUG];
  const { t } = useTranslate();
  const history = useHistory();
  const isAdminOrSupporter = useIsAdminOrSupporter();
  const organizationLoadable = useOrganizationLoadable();

  const handleOrganizationSelect = (organization: OrganizationModel) => {
    const redirectLink = isOwnerOrgRole(organization.currentUserRole)
      ? link
      : LINKS.ORGANIZATION_PROFILE;

    history.push(
      redirectLink.build({ [PARAMS.ORGANIZATION_SLUG]: organization.slug })
    );
  };

  const canManage = canManageOrganization(
    organizationLoadable.data?.currentUserRole,
    isAdminOrSupporter
  );
  const isAtLeastOrganizationMember = canViewOrganization(
    organizationLoadable.data?.currentUserRole,
    isAdminOrSupporter
  );

  const descriptors: Record<OrganizationMenuItemId, SettingsMenuItem> = {
    profile: {
      link: LINKS.ORGANIZATION_PROFILE.build({
        [PARAMS.ORGANIZATION_SLUG]: organizationSlug,
      }),
      label: t('organization_menu_profile'),
    },
    members: {
      link: LINKS.ORGANIZATION_MEMBERS.build({
        [PARAMS.ORGANIZATION_SLUG]: organizationSlug,
      }),
      label: t('organization_menu_members'),
    },
    'member-privileges': {
      link: LINKS.ORGANIZATION_MEMBER_PRIVILEGES.build({
        [PARAMS.ORGANIZATION_SLUG]: organizationSlug,
      }),
      label: t('organization_menu_member_privileges'),
    },
    glossaries: {
      link: LINKS.ORGANIZATION_GLOSSARIES.build({
        [PARAMS.ORGANIZATION_SLUG]: organizationSlug,
      }),
      label: t('organization_menu_glossaries'),
    },
    'translation-memories': {
      link: LINKS.ORGANIZATION_TRANSLATION_MEMORIES.build({
        [PARAMS.ORGANIZATION_SLUG]: organizationSlug,
      }),
      label: t(
        'organization_menu_translation_memories',
        'Translation memories'
      ),
    },
    apps: {
      link: LINKS.ORGANIZATION_APPS.build({
        [PARAMS.ORGANIZATION_SLUG]: organizationSlug,
      }),
      label: t('organization_menu_apps'),
    },
    'llm-providers': {
      link: LINKS.ORGANIZATION_LLM_PROVIDERS.build({
        [PARAMS.ORGANIZATION_SLUG]: organizationSlug,
      }),
      label: t('organization_menu_llm_providers'),
    },
    sso: {
      link: LINKS.ORGANIZATION_SSO.build({
        [PARAMS.ORGANIZATION_SLUG]: organizationSlug,
      }),
      label: t('organization_menu_sso_login'),
    },
    subscriptions: {
      link: LINKS.ORGANIZATION_SUBSCRIPTIONS.build({
        [PARAMS.ORGANIZATION_SLUG]: organizationSlug,
      }),
      label: t('organization_menu_subscriptions'),
    },
    invoices: {
      link: LINKS.ORGANIZATION_INVOICES.build({
        [PARAMS.ORGANIZATION_SLUG]: organizationSlug,
      }),
      label: t('organization_menu_invoices'),
    },
    'billing-test-clock': {
      link: LINKS.ORGANIZATION_BILLING_TEST_CLOCK_HELPER.build({
        [PARAMS.ORGANIZATION_SLUG]: organizationSlug,
      }),
      label: t('organization-menu-billing-test-clock'),
    },
  };

  const menuItems: SettingsMenuItem[] = organizationSettingsMenu({
    canManage,
    isAtLeastOrganizationMember,
    llmEnabled: config.llm.enabled,
    billingEnabled: config.billing.enabled,
    internalControllerEnabled: config.internalControllerEnabled,
  }).map((id) => ({ ...descriptors[id], dataCyItem: id }));

  const navigationPrefix: NavigationItem[] = organizationLoadable.data
    ? [[<OrganizationSwitch key={0} onSelect={handleOrganizationSelect} />]]
    : [];

  return (
    <BaseSettingsView
      {...otherProps}
      loading={organizationLoadable.isLoading || loading}
      navigation={[...navigationPrefix, ...(navigation || [])]}
      navigationRight={<CriticalUsageCircle />}
      menuItems={menuItems}
      hideChildrenOnLoading={false}
    >
      {children}
    </BaseSettingsView>
  );
};
