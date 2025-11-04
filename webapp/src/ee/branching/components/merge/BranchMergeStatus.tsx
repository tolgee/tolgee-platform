import React from 'react';
import { Box, Typography, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { CheckCircle, XClose } from '@untitled-ui/icons-react';

const StatusWrapper = styled(Box)`
  display: inline-flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(1)};
  color: ${({ theme }) => theme.palette.text.secondary};
`;

const ReadyIcon = styled(CheckCircle)`
  color: ${({ theme }) => theme.palette.success.main};
`;

const NotReadyIcon = styled(XClose)`
  color: ${({ theme }) => theme.palette.error.main};
`;

type Props = {
  ready: boolean;
};

export const BranchMergeStatus: React.FC<Props> = ({ ready }) => {
  const { t } = useTranslate();

  return (
    <StatusWrapper data-cy="project-branch-merge-status">
      {ready ? (
        <ReadyIcon width={22} height={22} />
      ) : (
        <NotReadyIcon width={22} height={22} />
      )}
      <Typography variant="body2" color="textSecondary">
        {ready && t('branch_merges_status_ready')}
      </Typography>
    </StatusWrapper>
  );
};
