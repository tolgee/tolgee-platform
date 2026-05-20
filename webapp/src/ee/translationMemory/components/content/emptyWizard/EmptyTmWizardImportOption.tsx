import React, { useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { UploadCloud02 } from '@untitled-ui/icons-react';
import { TranslationMemoryImportDialog } from 'tg.ee.module/translationMemory/components/content/TranslationMemoryImportDialog';
import { EmptyWizardCard } from 'tg.component/entriesList/EmptyWizardCard';

type Props = {
  organizationId: number;
  translationMemoryId: number;
  onFinished: () => void;
};

export const EmptyTmWizardImportOption: React.VFC<Props> = ({
  organizationId,
  translationMemoryId,
  onFinished,
}) => {
  const { t } = useTranslate();
  const [open, setOpen] = useState(false);

  return (
    <>
      <EmptyWizardCard
        dataCy="tm-empty-wizard-import"
        icon={<UploadCloud02 />}
        title={
          <T
            keyName="tm_empty_wizard_import_title"
            defaultValue="Import from file"
          />
        }
        description={t(
          'tm_empty_wizard_import_description',
          'Upload a TMX file with existing translations.'
        )}
        buttonLabel={
          <T
            keyName="tm_empty_wizard_import_button"
            defaultValue="Import file"
          />
        }
        onClick={() => setOpen(true)}
      />
      {open && (
        <TranslationMemoryImportDialog
          open={open}
          onClose={() => setOpen(false)}
          onFinished={() => {
            setOpen(false);
            onFinished();
          }}
          organizationId={organizationId}
          translationMemoryId={translationMemoryId}
          hasExistingEntries={false}
        />
      )}
    </>
  );
};
