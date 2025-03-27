import { Button } from '@mui/material';
import { Link } from 'react-router-dom';
import { LINKS, PARAMS } from 'tg.constants/links';
import React, { useState } from 'react';
import { BaseOrganizationSettingsView } from 'tg.views/organizations/components/BaseOrganizationSettingsView';
import { useTranslate } from '@tolgee/react';
import { useOrganization } from 'tg.views/organizations/useOrganization';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { GlossaryCreateDialog } from 'tg.ee.module/glossary/views/GlossaryCreateDialog';

export const GlossariesListView = () => {
  const organization = useOrganization();

  const { t } = useTranslate();

  const [createDialogOpen, setCreateDialogOpen] = useState(false);

  const glossaries = useApiQuery({
    url: '/v2/organizations/{organizationId}/glossaries',
    method: 'get',
    path: { organizationId: organization!.id },
    options: {
      keepPreviousData: true,
    },
  });

  const onCreate = () => {
    setCreateDialogOpen(true);
  };

  return (
    <BaseOrganizationSettingsView
      windowTitle={t('organization_glossaries_title')}
      link={LINKS.ORGANIZATION_GLOSSARIES}
      title=" "
      navigation={[
        [
          t('organization_glossaries_title'),
          LINKS.ORGANIZATION_GLOSSARIES.build({
            [PARAMS.ORGANIZATION_SLUG]: organization!.slug,
          }),
        ],
      ]}
      loading={glossaries.isLoading}
      hideChildrenOnLoading={false}
      maxWidth="normal"
      onAdd={onCreate}
    >
      {createDialogOpen && (
        <GlossaryCreateDialog
          open={createDialogOpen}
          onClose={() => setCreateDialogOpen(false)}
          onFinished={() => setCreateDialogOpen(false)}
          organizationId={organization!.id}
        />
      )}
      {glossaries.data &&
        glossaries.data.map((g) => (
          <>
            <div key={g.id}>{g.name}</div>
            <Button
              component={Link}
              to={LINKS.ORGANIZATION_GLOSSARY.build({
                [PARAMS.GLOSSARY_ID]: g.id,
                [PARAMS.ORGANIZATION_SLUG]: organization!.slug,
              })}
              size="medium"
              variant="outlined"
              style={{ marginBottom: '0.5rem' }}
              color="inherit"
            >
              View
            </Button>
          </>
        ))}
    </BaseOrganizationSettingsView>
  );
};
