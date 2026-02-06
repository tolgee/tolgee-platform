import { Button, Chip } from '@mui/material';
import clsx from 'clsx';
import { FC } from 'react';
import { T } from '@tolgee/react';
import { BranchMergeKeyModel } from '../../types';
import {
  AcceptButton,
  KeyFooterToggle,
  KeyHeader,
  KeyPanel,
  KeyWrapper,
} from './KeyPanelBase';
import { KeyTranslations } from './KeyTranslations';
import { MergeKeyHeader } from './MergeKeyHeader';

type ConflictPanelProps = {
  keyData: BranchMergeKeyModel;
  accepted?: boolean;
  onAccept?: () => void;
  changedTranslations?: string[];
  showAll?: boolean;
  onToggleShowAll?: () => void;
  hideAllWhenFalse?: boolean;
  toggleLabels?: {
    showAll: string;
    showLess: string;
  };
};

export const ConflictKeyPanel: FC<ConflictPanelProps> = ({
  keyData,
  accepted,
  onAccept,
  changedTranslations,
  showAll,
  onToggleShowAll,
  hideAllWhenFalse,
  toggleLabels,
}) => (
  <KeyWrapper className={clsx({ accepted })}>
    <KeyPanel>
      <KeyHeader className={clsx({ accepted })}>
        <MergeKeyHeader data={keyData} />
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
      <KeyTranslations
        keyData={keyData}
        changedTranslations={changedTranslations}
        showAll={showAll}
        hideAllWhenFalse={hideAllWhenFalse}
      />
    </KeyPanel>
    {onToggleShowAll && (
      <KeyFooterToggle
        showAll={showAll}
        onToggleShowAll={onToggleShowAll}
        toggleLabels={toggleLabels}
      />
    )}
  </KeyWrapper>
);
