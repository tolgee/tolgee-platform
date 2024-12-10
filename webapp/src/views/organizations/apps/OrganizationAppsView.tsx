import { FunctionComponent } from 'react';
import { useTranslate } from '@tolgee/react';
import { BaseOrganizationSettingsView } from '../components/BaseOrganizationSettingsView';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useOrganization } from '../useOrganization';
import { apps } from 'tg.ee';

export const OrganizationAppsView: FunctionComponent = () => {
  const organization = useOrganization();
  const { t } = useTranslate();

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
      {apps.map((App, index) => (
        <App key={index} />
      ))}
    </BaseOrganizationSettingsView>
  );
};
