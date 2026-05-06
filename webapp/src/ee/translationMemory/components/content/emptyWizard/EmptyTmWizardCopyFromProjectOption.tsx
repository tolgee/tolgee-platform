import React, { useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Folder } from '@untitled-ui/icons-react';
import { CopyFromProjectDialog } from 'tg.ee.module/translationMemory/components/content/CopyFromProjectDialog';
import { EmptyWizardCard } from 'tg.component/entriesList/EmptyWizardCard';

type Props = {
  organizationId: number;
  translationMemoryId: number;
  sourceLanguageTag: string;
  onFinished: () => void;
};

export const EmptyTmWizardCopyFromProjectOption: React.VFC<Props> = ({
  organizationId,
  translationMemoryId,
  sourceLanguageTag,
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
            keyName="tm_empty_wizard_connect_project_title"
            defaultValue="Use translations from a project"
          />
        }
        description={t(
          'tm_empty_wizard_connect_project_description',
          'Connect a project — its translations appear in this memory and stay in sync.'
        )}
        buttonLabel={
          <T
            keyName="tm_empty_wizard_connect_project_button"
            defaultValue="Connect project"
          />
        }
        onClick={() => setOpen(true)}
      />
      {open && (
        <CopyFromProjectDialog
          open={open}
          onClose={() => setOpen(false)}
          onFinished={() => {
            setOpen(false);
            onFinished();
          }}
          organizationId={organizationId}
          translationMemoryId={translationMemoryId}
          sourceLanguageTag={sourceLanguageTag}
        />
      )}
    </>
  );
};
