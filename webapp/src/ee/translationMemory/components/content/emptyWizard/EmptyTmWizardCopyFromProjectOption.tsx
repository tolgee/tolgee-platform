import React, { useState } from 'react';
import { T } from '@tolgee/react';
import { Folder } from '@untitled-ui/icons-react';
import { TranslationMemorySettingsDialog } from 'tg.ee.module/translationMemory/views/TranslationMemorySettingsDialog';
import { EmptyWizardCard } from 'tg.component/entriesList/EmptyWizardCard';

type Props = {
  translationMemoryId: number;
  assignedProjectsCount: number;
  onFinished: () => void;
};

export const EmptyTmWizardCopyFromProjectOption: React.VFC<Props> = ({
  translationMemoryId,
  assignedProjectsCount,
  onFinished,
}) => {
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
        description={
          assignedProjectsCount > 0 ? (
            <T
              keyName="tm_empty_wizard_sync_projects_description_connected"
              defaultValue="Connected to {count, plural, one {1 project} other {# projects}}. Translations will appear here once added."
              params={{ count: assignedProjectsCount }}
            />
          ) : (
            <T
              keyName="tm_empty_wizard_sync_projects_description"
              defaultValue="Choose which projects write into TM"
            />
          )
        }
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
          projectsOnly
        />
      )}
    </>
  );
};
