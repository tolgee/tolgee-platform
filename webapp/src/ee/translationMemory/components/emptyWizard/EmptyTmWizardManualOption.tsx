import React, { useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Plus } from '@untitled-ui/icons-react';
import { TranslationMemoryCreateEntryDialog } from 'tg.ee.module/translationMemory/views/TranslationMemoryCreateEntryDialog';
import { EmptyTmWizardCard } from './EmptyTmWizardCard';

type Props = {
  organizationId: number;
  translationMemoryId: number;
  sourceLanguageTag: string;
  availableLanguages: string[];
  onFinished: () => void;
};

export const EmptyTmWizardManualOption: React.VFC<Props> = ({
  organizationId,
  translationMemoryId,
  sourceLanguageTag,
  availableLanguages,
  onFinished,
}) => {
  const { t } = useTranslate();
  const [open, setOpen] = useState(false);

  return (
    <>
      <EmptyTmWizardCard
        dataCy="tm-empty-wizard-manual"
        icon={<Plus width={24} height={24} />}
        title={
          <T
            keyName="tm_empty_wizard_manual_title"
            defaultValue="Add manually"
          />
        }
        description={t(
          'tm_empty_wizard_manual_description',
          'Type a source segment and its translation. Good for one-off terminology.'
        )}
        onClick={() => setOpen(true)}
      />
      {open && (
        <TranslationMemoryCreateEntryDialog
          open={open}
          onClose={() => setOpen(false)}
          onFinished={() => {
            setOpen(false);
            onFinished();
          }}
          organizationId={organizationId}
          translationMemoryId={translationMemoryId}
          sourceLanguageTag={sourceLanguageTag}
          availableLanguages={availableLanguages}
        />
      )}
    </>
  );
};
