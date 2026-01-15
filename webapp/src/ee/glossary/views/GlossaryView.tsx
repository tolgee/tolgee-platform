import { LINKS, PARAMS } from 'tg.constants/links';
import { BaseOrganizationSettingsView } from 'tg.views/organizations/components/BaseOrganizationSettingsView';
import { useTranslate } from '@tolgee/react';
import React from 'react';
import { GlossaryViewBody } from 'tg.ee.module/glossary/components/GlossaryViewBody';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { useGlossary } from 'tg.ee.module/glossary/hooks/useGlossary';
import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { GlossarySwitch } from 'tg.ee.module/glossary/components/glossarySwitch';
import { GlossaryProjectsInfo } from 'tg.ee.module/glossary/components/GlossaryProjectsInfo';

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
      title={glossary.name || t('organization_glossary_title')}
      titleAdornment={
        <GlossaryProjectsInfo projects={glossary.assignedProjects} />
      }
      link={LINKS.ORGANIZATION_GLOSSARY}
      navigation={[
        [
          t('organization_glossaries_title'),
          LINKS.ORGANIZATION_GLOSSARIES.build({
            [PARAMS.ORGANIZATION_SLUG]: preferredOrganization?.slug || '',
          }),
        ],
        [<GlossarySwitch key="glossary-switch" />],
      ]}
      maxWidth="max"
      allCentered={false}
    >
      <GlossaryViewBody onSearch={setSearch} search={search} />
    </BaseOrganizationSettingsView>
  );
};
