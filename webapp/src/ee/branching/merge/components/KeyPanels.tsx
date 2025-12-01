import { Box, Button, Chip, styled } from '@mui/material';
import clsx from 'clsx';
import { T } from '@tolgee/react';
import { BranchMergeConflictModel, BranchMergeChangeModel } from '../types';
import { CellTranslation } from 'tg.views/projects/translations/TranslationsList/CellTranslation';
import { SimpleCellKey } from 'tg.views/projects/translations/SimpleCellKey';
import { useTranslationsSelector } from 'tg.views/projects/translations/context/TranslationsContext';
import { FC } from 'react';

const KeyPanel = styled(Box)`
  flex: 1;
  border: 1px solid ${({ theme }) => theme.palette.divider1};
  border-radius: ${({ theme }) => theme.spacing(1)};
  display: grid;

  &.accepted {
    border-color: ${({ theme }) => theme.palette.tokens.success.main};
  }
`;

const KeyHeader = styled(Box)`
  display: grid;
  grid-template-columns: 1fr auto;
  background: ${({ theme }) => theme.palette.tokens.background.hover};
  padding: ${({ theme }) => theme.spacing(1.5, 2)};

  &.accepted {
    background: ${({ theme }) => theme.palette.tokens.success._states.selected};
  }
`;

const AcceptButton = styled(Box)`
  display: flex;
  align-items: center;
`;

const TranslationList = styled(Box)`
  display: grid;
`;

const StyledLanguageField = styled('div')`
  border-color: ${({ theme }) => theme.palette.divider1};
  border-width: 1px 1px 1px 0;
  border-style: solid;

  & + & {
    border-top: 0;
  }
`;

export const KeyTranslations: FC<{
  keyData: any;
}> = ({ keyData }) => {
  const languages = useTranslationsSelector((c) => c.languages);
  return (
    <TranslationList>
      {Object.entries(keyData.translations ?? {}).map(([lang]) => {
        const language = languages?.find((l) => l.tag === lang);
        if (!language) return null;
        return (
          <StyledLanguageField
            key={lang}
            data-cy="translation-edit-translation-field"
          >
            <CellTranslation
              data={keyData}
              language={language}
              active={false}
              lastFocusable={false}
              readonly={true}
            />
          </StyledLanguageField>
        );
      })}
    </TranslationList>
  );
};

type ConflictPanelProps = {
  keyData: any;
  conflict?: BranchMergeConflictModel | BranchMergeChangeModel;
  accepted?: boolean;
  onAccept?: () => void;
};

export const ConflictKeyPanel: FC<ConflictPanelProps> = ({
  keyData,
  accepted,
  onAccept,
}) => (
  <KeyPanel className={clsx({ accepted })}>
    <KeyHeader className={clsx({ accepted })}>
      <SimpleCellKey data={keyData} />
      <AcceptButton>
        {accepted ? (
          <Chip
            size="small"
            color="success"
            label={<T keyName="branch_merges_conflict_accepted" />}
          />
        ) : onAccept ? (
          <Button
            size="small"
            variant="outlined"
            onClick={onAccept}
            data-cy="project-branch-merge-accept"
          >
            <T keyName="branch_merges_accept" />
          </Button>
        ) : null}
      </AcceptButton>
    </KeyHeader>
    <KeyTranslations keyData={keyData} />
  </KeyPanel>
);

export const SingleKeyPanel: FC<{ keyData: any }> = ({ keyData }) => (
  <KeyPanel>
    <KeyHeader>
      <SimpleCellKey data={keyData} />
    </KeyHeader>
    <KeyTranslations keyData={keyData} />
  </KeyPanel>
);
