import React, { useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Folder } from '@untitled-ui/icons-react';
import { CopyFromProjectDialog } from 'tg.ee.module/translationMemory/components/content/CopyFromProjectDialog';
import { EmptyTmWizardCard } from './EmptyTmWizardCard';

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
      <EmptyTmWizardCard
        dataCy="tm-empty-wizard-copy"
        icon={<Folder />}
        title={
          <T
            keyName="tm_empty_wizard_copy_card_title"
            defaultValue="Copy from a project"
          />
        }
        description={t(
          'tm_empty_wizard_copy_card_description',
          "Seed this memory with entries from an existing project's TM."
        )}
        buttonLabel={
          <T
            keyName="tm_empty_wizard_copy_card_button"
            defaultValue="Choose project"
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
