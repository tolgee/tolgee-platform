import { useState } from 'react';
import { Checkbox, FormControlLabel } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { useTranslationsSelector } from 'tg.views/projects/translations/context/TranslationsContext';
import { OperationProps } from 'tg.views/projects/translations/BatchOperations/types';
import { BatchOperationsSubmit } from 'tg.views/projects/translations/BatchOperations/components/BatchOperationsSubmit';
import { OperationContainer } from 'tg.views/projects/translations/BatchOperations/components/OperationContainer';
import { BatchOperationsLanguagesSelect } from 'tg.views/projects/translations/BatchOperations/components/BatchOperationsLanguagesSelect';
import { getPreselectedLanguages } from 'tg.views/projects/translations/BatchOperations/getPreselectedLanguages';

import { AiReviewRunDialog } from 'tg.ee.module/aiReview/runs/AiReviewRunDialog';

export const OperationAiReview = ({ disabled, onStart }: OperationProps) => {
  const { t } = useTranslate();
  const allLanguages = useTranslationsSelector((c) => c.languages) || [];
  const translationsLanguages = useTranslationsSelector(
    (c) => c.translationsLanguages
  );
  const selection = useTranslationsSelector((c) => c.selection);

  const languages = allLanguages.filter((l) => !l.base);

  const [selectedLangs, setSelectedLangs] = useState<string[]>(() =>
    getPreselectedLanguages(languages, translationsLanguages ?? [])
  );
  const [reReview, setReReview] = useState(false);
  const [dialogOpen, setDialogOpen] = useState(false);

  return (
    <OperationContainer>
      <BatchOperationsLanguagesSelect
        languages={languages}
        value={selectedLangs}
        onChange={setSelectedLangs}
        languagePermission="translations.state-edit"
      />
      <FormControlLabel
        control={
          <Checkbox
            size="small"
            checked={reReview}
            onChange={(_, checked) => setReReview(checked)}
            data-cy="ai-review-batch-re-review"
          />
        }
        label={t(
          'ai_review_batch_re_review_label',
          'Also re-review translations that already have a result'
        )}
      />
      <BatchOperationsSubmit
        loading={false}
        disabled={disabled || selectedLangs.length === 0}
        onClick={() => setDialogOpen(true)}
      />
      {dialogOpen && (
        <AiReviewRunDialog
          variant="review"
          request={{
            keyIds: selection,
            languageIds: allLanguages
              .filter((l) => selectedLangs.includes(l.tag))
              .map((l) => l.id),
            reReview,
          }}
          onClose={() => setDialogOpen(false)}
          onStarted={onStart}
        />
      )}
    </OperationContainer>
  );
};
