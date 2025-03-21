import { LINKS, PARAMS } from 'tg.constants/links';
import { useOrganization } from 'tg.views/organizations/useOrganization';
import { BaseOrganizationSettingsView } from 'tg.views/organizations/components/BaseOrganizationSettingsView';
import { useTranslate } from '@tolgee/react';

export const GlossaryView = () => {
  const organization = useOrganization();

  const { t } = useTranslate();

  return (
    <BaseOrganizationSettingsView
      windowTitle={t('organization_glossaries_title')}
      link={LINKS.ORGANIZATION_GLOSSARIES}
      title={t('organization_glossaries_title')}
      navigation={[
        [
          t('edit_organization_title'),
          LINKS.ORGANIZATION_GLOSSARIES.build({
            [PARAMS.ORGANIZATION_SLUG]: organization!.slug,
          }),
        ],
      ]}
      hideChildrenOnLoading={false}
      maxWidth="normal"
    >
      <h1>Organization Glossary</h1>
    </BaseOrganizationSettingsView>
  );
};
