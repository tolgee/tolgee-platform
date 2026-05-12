import React from 'react';
import { Box, styled } from '@mui/material';
import { EmptyTmWizardManualOption } from './EmptyTmWizardManualOption';
import { EmptyTmWizardCopyFromProjectOption } from './EmptyTmWizardCopyFromProjectOption';
import { EmptyTmWizardImportOption } from './EmptyTmWizardImportOption';

const StyledBox = styled(Box)`
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: ${({ theme }) => theme.spacing(2)};
  margin: ${({ theme }) => theme.spacing(2)} auto;
  max-width: 1100px;
  text-align: center;

  ${({ theme }) => theme.breakpoints.down('md')} {
    grid-template-columns: 1fr;
    max-width: 490px;
  }
`;

type Props = {
  organizationId: number;
  translationMemoryId: number;
  sourceLanguageTag: string;
  allLanguageTags: string[];
  initialSelectedTags: string[];
  assignedProjectsCount: number;
  onFinished: () => void;
};

export const EmptyTmWizard: React.VFC<Props> = ({
  organizationId,
  translationMemoryId,
  sourceLanguageTag,
  allLanguageTags,
  initialSelectedTags,
  assignedProjectsCount,
  onFinished,
}) => {
  return (
    <StyledBox data-cy="tm-empty-wizard">
      <EmptyTmWizardCopyFromProjectOption
        organizationId={organizationId}
        translationMemoryId={translationMemoryId}
        sourceLanguageTag={sourceLanguageTag}
        assignedProjectsCount={assignedProjectsCount}
        onFinished={onFinished}
      />
      <EmptyTmWizardImportOption
        organizationId={organizationId}
        translationMemoryId={translationMemoryId}
        onFinished={onFinished}
      />
      <EmptyTmWizardManualOption
        organizationId={organizationId}
        translationMemoryId={translationMemoryId}
        sourceLanguageTag={sourceLanguageTag}
        allLanguageTags={allLanguageTags}
        initialSelectedTags={initialSelectedTags}
        onFinished={onFinished}
      />
    </StyledBox>
  );
};
