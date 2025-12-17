import { Button, Chip } from '@mui/material';
import clsx from 'clsx';
import { FC } from 'react';
import { T } from '@tolgee/react';
import { BranchMergeConflictModel, BranchMergeChangeModel } from '../../types';
import { AcceptButton, KeyHeader, KeyPanel } from './KeyPanelBase';
import { KeyTranslations } from './KeyTranslations';
import { SimpleCellKey } from 'tg.views/projects/translations/SimpleCellKey';

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
