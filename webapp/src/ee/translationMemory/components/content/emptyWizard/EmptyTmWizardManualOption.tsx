import React, { useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { PlusCircle } from '@untitled-ui/icons-react';
import { TranslationMemoryCreateEntryDialog } from 'tg.ee.module/translationMemory/views/TranslationMemoryCreateEntryDialog';
import { EmptyWizardCard } from 'tg.component/entriesList/EmptyWizardCard';

type Props = {
  organizationId: number;
  translationMemoryId: number;
  sourceLanguageTag: string;
  allLanguageTags: string[];
  initialSelectedTags: string[];
  onFinished: () => void;
};

export const EmptyTmWizardManualOption: React.VFC<Props> = ({
  organizationId,
  translationMemoryId,
  sourceLanguageTag,
  allLanguageTags,
  initialSelectedTags,
  onFinished,
}) => {
  const { t } = useTranslate();
  const [open, setOpen] = useState(false);

  return (
    <>
      <EmptyWizardCard
        dataCy="tm-empty-wizard-manual"
        icon={<PlusCircle />}
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
        buttonLabel={
          <T keyName="tm_empty_wizard_manual_button" defaultValue="Add entry" />
        }
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
          allLanguageTags={allLanguageTags}
          initialSelectedTags={initialSelectedTags}
        />
      )}
    </>
  );
};
