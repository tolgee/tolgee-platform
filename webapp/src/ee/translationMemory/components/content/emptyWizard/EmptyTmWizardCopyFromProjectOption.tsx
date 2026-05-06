import React, { useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Folder } from '@untitled-ui/icons-react';
import { TranslationMemorySettingsDialog } from 'tg.ee.module/translationMemory/views/TranslationMemorySettingsDialog';
import { EmptyWizardCard } from 'tg.component/entriesList/EmptyWizardCard';

type Props = {
  organizationId: number;
  translationMemoryId: number;
  sourceLanguageTag: string;
  onFinished: () => void;
};

export const EmptyTmWizardCopyFromProjectOption: React.VFC<Props> = ({
  translationMemoryId,
  onFinished,
}) => {
  const { t } = useTranslate();
  const [open, setOpen] = useState(false);

  return (
    <>
      <EmptyWizardCard
        dataCy="tm-empty-wizard-copy"
        icon={<Folder />}
        title={
          <T
            keyName="tm_empty_wizard_sync_projects_title"
            defaultValue="Sync from projects"
          />
        }
        description={t(
          'tm_empty_wizard_sync_projects_description',
          'Choose which projects write into TM'
        )}
        buttonLabel={
          <T
            keyName="tm_empty_wizard_sync_projects_button"
            defaultValue="Manage projects"
          />
        }
        onClick={() => setOpen(true)}
      />
      {open && (
        <TranslationMemorySettingsDialog
          open={open}
          onClose={() => setOpen(false)}
          onFinished={() => {
            setOpen(false);
            onFinished();
          }}
          translationMemoryId={translationMemoryId}
        />
      )}
    </>
  );
};
