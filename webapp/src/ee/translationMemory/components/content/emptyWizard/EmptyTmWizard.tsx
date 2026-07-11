import React from 'react';
import { Box, styled } from '@mui/material';
import { EmptyTmWizardManualOption } from './EmptyTmWizardManualOption';
import { EmptyTmWizardCopyFromProjectOption } from './EmptyTmWizardCopyFromProjectOption';
import { EmptyTmWizardImportOption } from './EmptyTmWizardImportOption';

const StyledBox = styled(Box)<{ $cardCount: number }>`
  display: grid;
  grid-template-columns: ${({ $cardCount }) =>
    `repeat(${$cardCount}, minmax(0, 1fr))`};
  gap: ${({ theme }) => theme.spacing(2)};
  margin: ${({ theme }) => theme.spacing(2)} auto;
  max-width: ${({ $cardCount }) => ($cardCount === 3 ? 1100 : 740)}px;
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
  /** PROJECT-type TMs have no assignment editor — the Sync card opens one, so hide it. */
  isProjectTm: boolean;
  onFinished: () => void;
};

export const EmptyTmWizard: React.VFC<Props> = ({
  organizationId,
  translationMemoryId,
  sourceLanguageTag,
  allLanguageTags,
  initialSelectedTags,
  assignedProjectsCount,
  isProjectTm,
  onFinished,
}) => {
  const cardCount = isProjectTm ? 2 : 3;
  return (
    <StyledBox data-cy="tm-empty-wizard" $cardCount={cardCount}>
      {!isProjectTm && (
        <EmptyTmWizardCopyFromProjectOption
          translationMemoryId={translationMemoryId}
          assignedProjectsCount={assignedProjectsCount}
          onFinished={onFinished}
        />
      )}
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
