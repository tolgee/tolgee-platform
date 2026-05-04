import React, { useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { UploadCloud02 } from '@untitled-ui/icons-react';
import { TranslationMemoryImportDialog } from 'tg.ee.module/translationMemory/components/TranslationMemoryImportDialog';
import { EmptyTmWizardCard } from './EmptyTmWizardCard';

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
      <EmptyTmWizardCard
        dataCy="tm-empty-wizard-import"
        icon={<UploadCloud02 width={24} height={24} />}
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
