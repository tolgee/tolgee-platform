import React from 'react';
import { styled, Typography } from '@mui/material';
import { T } from '@tolgee/react';
import { EmptyTmWizardManualOption } from './EmptyTmWizardManualOption';
import { EmptyTmWizardCopyFromProjectOption } from './EmptyTmWizardCopyFromProjectOption';
import { EmptyTmWizardImportOption } from './EmptyTmWizardImportOption';

const StyledWrapper = styled('div')`
  background: ${({ theme }) => theme.palette.background.paper};
  border: 1px dashed ${({ theme }) => theme.palette.divider};
  border-radius: 12px;
  padding: 36px 28px;
  margin-top: 8px;
`;

const StyledHead = styled('div')`
  margin-bottom: 24px;
  text-align: center;
`;

const StyledCards = styled('div')`
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  max-width: 880px;
  margin: 0 auto;

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
 * state. The wizard itself does layout only.
 */
export const EmptyTmWizard: React.VFC<Props> = ({
  organizationId,
  translationMemoryId,
  sourceLanguageTag,
  availableLanguages,
  onFinished,
}) => {
  return (
    <StyledWrapper data-cy="tm-empty-wizard">
      <StyledHead>
        <Typography
          variant="h6"
          fontWeight={500}
          color="text.primary"
          sx={{ mb: 0.75 }}
        >
          <T
            keyName="tm_empty_wizard_title"
            defaultValue="This memory is empty"
          />
        </Typography>
        <Typography
          variant="body2"
          color="text.secondary"
          sx={{ lineHeight: 1.5 }}
        >
          <T
            keyName="tm_empty_wizard_subtitle"
            defaultValue="Add your first entries to start matching translations against this memory."
          />
        </Typography>
      </StyledHead>
      <StyledCards>
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
      </StyledCards>
    </StyledWrapper>
  );
};
