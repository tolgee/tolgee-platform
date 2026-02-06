import { Box, styled, Typography, useTheme } from '@mui/material';
import {
  AlertTriangle,
  CheckCircle,
  MinusCircle,
  PlusCircle,
  RefreshCcw02,
} from '@untitled-ui/icons-react';
import { FC } from 'react';
import { useTranslate } from '@tolgee/react';
import { BranchMergeModel } from '../../types';

const StatsRow = styled(Box)`
  display: flex;
  flex-wrap: wrap;
  gap: ${({ theme }) => theme.spacing(3)};
`;

const StatCard = styled(Box)`
  display: flex;
  flex: 1;
  justify-content: center;
  align-items: center;
  flex-direction: column;
  border-radius: 20px;
  gap: ${({ theme }) => theme.spacing(0.5)};
  padding: ${({ theme }) => theme.spacing(2)};
  background-color: ${({ theme }) => theme.palette.tokens.background.hover};
`;

export const StatsBlock: FC<{ merge: BranchMergeModel }> = ({ merge }) => {
  const { t } = useTranslate();
  const theme = useTheme();

  const conflictsTotal =
    merge.keyUnresolvedConflictsCount + merge.keyResolvedConflictsCount;
  const resolvedConflicts = merge.keyResolvedConflictsCount;

  return (
    <StatsRow>
      <StatCard data-cy="merge-stat-additions">
        <Box display="flex" alignItems="center" gap={1} flexGrow={0}>
          <Box display="flex" alignItems="center">
            <PlusCircle />
          </Box>
          <Typography variant="h3">
            <div data-cy="merge-stat-additions-count">
              {merge.keyAdditionsCount}
            </div>
          </Typography>
        </Box>
        <Typography variant="body1" fontSize={18}>
          {t('branch_merges_additions')}
        </Typography>
      </StatCard>
      <StatCard data-cy="merge-stat-modifications">
        <Box display="flex" alignItems="center" gap={1} flexGrow={0}>
          <Box display="flex" alignItems="center">
            <RefreshCcw02 />
          </Box>
          <Typography variant="h3">
            <div data-cy="merge-stat-modifications-count">
              {merge.keyModificationsCount}
            </div>
          </Typography>
        </Box>
        <Typography variant="body1" fontSize={18}>
          {t('branch_merges_modifications')}
        </Typography>
      </StatCard>
      <StatCard data-cy="merge-stat-deletions">
        <Box display="flex" alignItems="center" gap={1} flexGrow={0}>
          <Box display="flex" alignItems="center">
            <MinusCircle />
          </Box>
          <Typography variant="h3">
            <div data-cy="merge-stat-deletions-count">
              {merge.keyDeletionsCount}
            </div>
          </Typography>
        </Box>
        <Typography variant="body1" fontSize={18}>
          {t('branch_merges_deletions')}
        </Typography>
      </StatCard>
      <StatCard data-cy="merge-stat-conflicts">
        <Box display="flex" alignItems="center" gap={1} flexGrow={0}>
          <Box display="flex" alignItems="center">
            {conflictsTotal > 0 && resolvedConflicts === conflictsTotal ? (
              <CheckCircle color={theme.palette.tokens.success.main} />
            ) : (
              <AlertTriangle
                color={
                  conflictsTotal > 0
                    ? theme.palette.tokens.warning.main
                    : 'default'
                }
              />
            )}
          </Box>
          <Typography variant="h3">
            <div data-cy="merge-stat-conflicts-count">
              {resolvedConflicts + '/' + conflictsTotal}
            </div>
          </Typography>
        </Box>
        <Typography variant="body1" fontSize={18}>
          {conflictsTotal > 0
            ? t('branch_merges_conflicts_resolved_unresolved')
            : t('branch_merges_conflicts')}
        </Typography>
      </StatCard>
    </StatsRow>
  );
};
