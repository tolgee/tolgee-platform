import React from 'react';
import { Box, styled } from '@mui/material';
import { EmptyTmWizardManualOption } from './EmptyTmWizardManualOption';
import { EmptyTmWizardCopyFromProjectOption } from './EmptyTmWizardCopyFromProjectOption';
import { EmptyTmWizardImportOption } from './EmptyTmWizardImportOption';

// Grid (with `minmax(0, 1fr)`) instead of flex with `flex: 1 1 280px`. Flexbox sizes each
// card past its content min-width, so a card with longer text ends up wider than its
// siblings (e.g. "Add manually" was visibly larger than the other two). Grid splits the
// row into exactly equal columns regardless of inner copy.
const StyledBox = styled(Box)`
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: ${({ theme }) => theme.spacing(2)};
  margin: ${({ theme }) => theme.spacing(2)};
  text-align: center;

  ${({ theme }) => theme.breakpoints.down('md')} {
    grid-template-columns: 1fr;
  }
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
