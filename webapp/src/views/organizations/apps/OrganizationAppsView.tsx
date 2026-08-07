import { FunctionComponent } from 'react';
import { Box, Tab, Tabs, styled } from '@mui/material';
import { Link, useRouteMatch } from 'react-router-dom';
import { useTranslate } from '@tolgee/react';
import { BaseOrganizationSettingsView } from '../components/BaseOrganizationSettingsView';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useConfig } from 'tg.globalContext/helpers';
import { useOrganization } from '../useOrganization';
import { apps } from 'tg.ee';
import { RegisteredAppsSection } from 'tg.views/organizations/apps/registeredApps/RegisteredAppsSection';
import { OwnedAppsSection } from 'tg.views/organizations/apps/ownedApps/OwnedAppsSection';

const StyledTabs = styled(Tabs)`
  margin-bottom: -1px;
`;

const StyledTabWrapper = styled(Box)`
  border-bottom: 1px solid ${({ theme }) => theme.palette.divider1};
`;

export const OrganizationAppsView: FunctionComponent<
  React.PropsWithChildren<unknown>
> = () => {
  const organization = useOrganization();
  const config = useConfig();
  const { t } = useTranslate();
  const ownedMatch = useRouteMatch(LINKS.ORGANIZATION_APPS_OWNED.template);

  const ownedTabVisible = Boolean(
    config.appsEnabled && organization?.currentUserRole === 'OWNER'
  );
  const tab = ownedTabVisible && ownedMatch?.isExact ? 'owned' : 'installed';

  if (!organization) {
    return null;
  }

  return (
    <BaseOrganizationSettingsView
      windowTitle={t('organization_apps_title')}
      link={LINKS.ORGANIZATION_APPS}
      title={t('organization_apps_title')}
      navigation={[
        [
          t('edit_organization_title'),
          LINKS.ORGANIZATION_APPS.build({
            [PARAMS.ORGANIZATION_SLUG]: organization.slug,
          }),
        ],
      ]}
      hideChildrenOnLoading={false}
      maxWidth="normal"
    >
      {ownedTabVisible && (
        <StyledTabWrapper>
          <StyledTabs value={tab}>
            <Tab
              value="installed"
              component={Link}
              to={LINKS.ORGANIZATION_APPS.build({
                [PARAMS.ORGANIZATION_SLUG]: organization.slug,
              })}
              label={t('organization_apps_tab_installed', 'Installed apps')}
              data-cy="organization-apps-tab-installed"
            />
            <Tab
              value="owned"
              component={Link}
              to={LINKS.ORGANIZATION_APPS_OWNED.build({
                [PARAMS.ORGANIZATION_SLUG]: organization.slug,
              })}
              label={t('organization_apps_tab_owned', 'Owned apps')}
              data-cy="organization-apps-tab-owned"
            />
          </StyledTabs>
        </StyledTabWrapper>
      )}

      <Box display="grid" gap={2} mt={ownedTabVisible ? 2 : 0}>
        {tab === 'installed' && (
          <>
            {apps.map((App, index) => (
              <App key={index} />
            ))}
            {config.appsEnabled && <RegisteredAppsSection />}
          </>
        )}

        {tab === 'owned' && (
          <OwnedAppsSection organizationId={organization.id} />
        )}
      </Box>
    </BaseOrganizationSettingsView>
  );
};
