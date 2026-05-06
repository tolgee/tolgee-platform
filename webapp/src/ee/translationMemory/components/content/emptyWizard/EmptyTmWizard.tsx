import React from 'react';
import { Box, styled } from '@mui/material';
import { EmptyTmWizardManualOption } from './EmptyTmWizardManualOption';
import { EmptyTmWizardCopyFromProjectOption } from './EmptyTmWizardCopyFromProjectOption';
import { EmptyTmWizardImportOption } from './EmptyTmWizardImportOption';

const StyledBox = styled(Box)`
  display: flex;
  gap: ${({ theme }) => theme.spacing(2)};
  align-items: center;
  justify-content: center;
  text-align: center;
  margin: ${({ theme }) => theme.spacing(2)};
  flex-wrap: wrap;
`;

type Props = {
  organizationId: number;
  translationMemoryId: number;
  sourceLanguageTag: string;
  availableLanguages: string[];
  onFinished: () => void;
};

/**
 * Empty-state replacement for the entries table — shown when a TM has no entries yet and the
 * viewer can manage entries. Each card is a self-contained option that owns its own dialog
 * state. The wizard itself does layout only. Visual matches GlossaryEmptyListMessage so empty
 * states across the platform share the same shape.
 */
export const EmptyTmWizard: React.VFC<Props> = ({
  organizationId,
  translationMemoryId,
  sourceLanguageTag,
  availableLanguages,
  onFinished,
}) => {
  return (
    <StyledBox data-cy="tm-empty-wizard">
      <EmptyTmWizardManualOption
        organizationId={organizationId}
        translationMemoryId={translationMemoryId}
        sourceLanguageTag={sourceLanguageTag}
        availableLanguages={availableLanguages}
        onFinished={onFinished}
      />
      <EmptyTmWizardCopyFromProjectOption
        organizationId={organizationId}
        translationMemoryId={translationMemoryId}
        sourceLanguageTag={sourceLanguageTag}
        onFinished={onFinished}
      />
      <EmptyTmWizardImportOption
        organizationId={organizationId}
        translationMemoryId={translationMemoryId}
        onFinished={onFinished}
      />
    </StyledBox>
  );
};
