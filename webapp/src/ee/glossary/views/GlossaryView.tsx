import { LINKS, PARAMS } from 'tg.constants/links';
import { BaseOrganizationSettingsView } from 'tg.views/organizations/components/BaseOrganizationSettingsView';
import { useTranslate } from '@tolgee/react';
import React, { useState } from 'react';
import { GlossaryTermCreateDialog } from 'tg.ee.module/glossary/views/GlossaryTermCreateDialog';
import { GlossaryViewBody } from 'tg.ee.module/glossary/components/GlossaryViewBody';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { useGlossary } from 'tg.ee.module/glossary/hooks/useGlossary';
import { usePreferredOrganization } from 'tg.globalContext/helpers';

export const GlossaryView = () => {
  const [search, setSearch] = useUrlSearchState('search', {
    defaultVal: '',
  });
  const [createDialogOpen, setCreateDialogOpen] = useState(false);

  const { preferredOrganization } = usePreferredOrganization();
  const glossary = useGlossary();

  const { t } = useTranslate();

  const onCreate = () => {
    setCreateDialogOpen(true);
  };

  const canCreate = ['OWNER', 'MAINTAINER'].includes(
    preferredOrganization?.currentUserRole || ''
  );

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
      {canCreate && createDialogOpen && preferredOrganization !== undefined && (
        <GlossaryTermCreateDialog
          open={createDialogOpen}
          onClose={() => setCreateDialogOpen(false)}
          onFinished={() => setCreateDialogOpen(false)}
        />
      )}
      <GlossaryViewBody
        onCreate={canCreate ? onCreate : undefined}
        onSearch={setSearch}
        search={search}
      />
    </BaseOrganizationSettingsView>
  );
};
