import { Box, Typography, styled, useTheme } from '@mui/material';
import {
  RefreshCcw02,
  Plus,
  Minus,
  CheckCircle,
  AlertTriangle,
} from '@untitled-ui/icons-react';
import { ElementType, FC, useMemo } from 'react';
import { useTranslate } from '@tolgee/react';
import { BranchMergeModel } from '../types';

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
  const baseStats = useMemo(
    () => [
      {
        label: t('branch_merges_additions'),
        value: merge.keyAdditionsCount,
        icon: Plus,
      },
      {
        label: t('branch_merges_modifications'),
        value: merge.keyModificationsCount,
        icon: RefreshCcw02,
      },
      {
        label: t('branch_merges_deletions'),
        value: merge.keyDeletionsCount,
        icon: Minus,
      },
    ],
    [merge, t]
  );

  const conflictsTotal =
    merge.keyUnresolvedConflictsCount + merge.keyResolvedConflictsCount;
  const resolvedConflicts = merge.keyResolvedConflictsCount;

  return (
    <StatsRow>
      {baseStats.map((stat) => {
        const Icon = stat.icon as ElementType;
        return (
          <StatCard key={stat.label}>
            <Box display="flex" alignItems="center" gap={1} flexGrow={0}>
              <Box display="flex" alignItems="center">
                <Icon />
              </Box>
              <Typography variant="h3">
                <div>{stat.value}</div>
              </Typography>
            </Box>
            <Typography variant="body1" fontSize={18}>
              {stat.label}
            </Typography>
          </StatCard>
        );
      })}
      <StatCard>
        <Box display="flex" alignItems="center" gap={1} flexGrow={0}>
          <Box display="flex" alignItems="center">
            {conflictsTotal > 0 &&
              (resolvedConflicts === conflictsTotal ? (
                <CheckCircle color={theme.palette.tokens.success.main} />
              ) : (
                <AlertTriangle color={theme.palette.tokens.warning.main} />
              ))}
          </Box>
          <Typography variant="h3">
            <div>{resolvedConflicts + '/' + conflictsTotal}</div>
          </Typography>
        </Box>
        <Typography variant="body1" fontSize={18}>
          {t('branch_merges_conflicts_resolved_unresolved')}
        </Typography>
      </StatCard>
    </StatsRow>
  );
};
