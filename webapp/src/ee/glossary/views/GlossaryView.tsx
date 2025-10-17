import { LINKS, PARAMS } from 'tg.constants/links';
import { BaseOrganizationSettingsView } from 'tg.views/organizations/components/BaseOrganizationSettingsView';
import { useTranslate } from '@tolgee/react';
import React from 'react';
import { GlossaryViewBody } from 'tg.ee.module/glossary/components/GlossaryViewBody';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { useGlossary } from 'tg.ee.module/glossary/hooks/useGlossary';
import { usePreferredOrganization } from 'tg.globalContext/helpers';

export const GlossaryView = () => {
  const [search, setSearch] = useUrlSearchState('search', {
    defaultVal: '',
  });

  const { preferredOrganization } = usePreferredOrganization();
  const glossary = useGlossary();

  const { t } = useTranslate();

  return (
    <BaseOrganizationSettingsView
      windowTitle={glossary.name || t('organization_glossary_title')}
      link={LINKS.ORGANIZATION_GLOSSARY}
      navigation={[
        [
          t('organization_glossaries_title'),
          LINKS.ORGANIZATION_GLOSSARIES.build({
            [PARAMS.ORGANIZATION_SLUG]: preferredOrganization?.slug || '',
          }),
        ],
        [
          glossary.name || t('organization_glossary_view_title'),
          LINKS.ORGANIZATION_GLOSSARY.build({
            [PARAMS.GLOSSARY_ID]: glossary.id,
            [PARAMS.ORGANIZATION_SLUG]: preferredOrganization?.slug || '',
          }),
        ],
      ]}
      maxWidth="max"
      allCentered={false}
    >
      <GlossaryViewBody onSearch={setSearch} search={search} />
    </BaseOrganizationSettingsView>
  );
};
