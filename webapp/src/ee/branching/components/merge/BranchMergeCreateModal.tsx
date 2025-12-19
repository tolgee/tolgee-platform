import React from 'react';
import { T, useTranslate } from '@tolgee/react';
import { components } from 'tg.service/apiSchema.generated';
import ConfirmationDialog from 'tg.component/common/ConfirmationDialog';
import { BranchNameChipNode } from 'tg.component/branching/BranchNameChip';

type BranchModel = components['schemas']['BranchModel'];
type DryRunMergeBranchRequest = { sourceBranchId: number };

type Props = {
  open: boolean;
  submit: (values: DryRunMergeBranchRequest) => void;
  sourceBranch?: BranchModel | null;
};

export const BranchMergeCreateModal: React.FC<Props> = ({
  open,
  submit,
  sourceBranch,
}) => {
  const { t } = useTranslate();

  const handleSubmit = () => {
    if (sourceBranch) {
      submit({
        sourceBranchId: sourceBranch.id,
      });
    }
  };

  return (
    <ConfirmationDialog
      onConfirm={handleSubmit}
      open={open}
      message={
        <T
          keyName="branch_merges_create_title"
          params={{
            name: sourceBranch?.name,
            branch: <BranchNameChipNode />,
            targetName: sourceBranch?.originBranchName,
          }}
        />
      }
      confirmButtonText={t('branch_merges_create_button')}
    />
  );
};
