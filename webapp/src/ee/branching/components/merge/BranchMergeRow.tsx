import React from 'react';
import { ArrowRight, Trash01 } from '@untitled-ui/icons-react';
import { useTranslate } from '@tolgee/react';
import {
  Box,
  IconButton,
  Paper,
  Tooltip,
  Typography,
  styled,
  useTheme,
} from '@mui/material';

import { BranchNameChip } from 'tg.ee.module/branching/components/BranchNameChip';
import { BranchMergeStatus } from 'tg.ee.module/branching/components/merge/BranchMergeStatus';
import { components } from 'tg.service/apiSchema.generated';

type BranchMergeModel = components['schemas']['BranchMergeModel'];

type Props = {
  merge: BranchMergeModel;
  onDelete?: () => void;
  deleting?: boolean;
};

const RowCard = styled(Paper)`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(3)};
  padding: ${({ theme }) => theme.spacing(2.5, 3)};
  border-radius: ${({ theme }) => theme.spacing(1.5)};
  flex-wrap: wrap;
  width: 100%;
`;

const BranchesSection = styled(Box)`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(1.5)};
  min-width: 220px;
`;

const StyledArrow = styled(ArrowRight)`
  color: ${({ theme }) => theme.palette.text.disabled};
`;

const StatsSection = styled(Box)`
  display: flex;
  flex-wrap: wrap;
  gap: ${({ theme }) => theme.spacing(3)};
  flex: 1;
  min-width: 220px;
`;

const StatCard = styled(Box)`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.spacing(0.5)};
`;

const StatusSection = styled(Box)`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(1.5)};
`;

export const BranchMergeRow: React.FC<Props> = ({
  merge,
  onDelete,
  deleting,
}: Props) => {
  const theme = useTheme();
  const { t } = useTranslate();

  const stats = [
    {
      key: 'additions',
      label: t('branch_merges_additions'),
      value: merge.keyAdditionsCount,
      color: theme.palette.text.secondary,
    },
    {
      key: 'deletions',
      label: t('branch_merges_deletions'),
      value: merge.keyDeletionsCount,
      color: theme.palette.text.secondary,
    },
    {
      key: 'modifications',
      label: t('branch_merges_modifications'),
      value: merge.keyModificationsCount,
      color: theme.palette.text.secondary,
    },
    {
      key: 'conflicts',
      label: t('branch_merges_conflicts'),
      value: merge.keyUnresolvedConflictsCount,
      color:
        merge.keyUnresolvedConflictsCount > 0
          ? theme.palette.error.main
          : theme.palette.text.secondary,
    },
  ];

  const readyToMerge =
    merge.keyUnresolvedConflictsCount === 0 && !merge.outdated;

  return (
    <RowCard data-cy="project-branch-merge-item" variant="outlined">
      <Box>
        <Typography variant="subtitle1" mb={1}>
          {merge.name}
        </Typography>
        <BranchesSection>
          <BranchNameChip name={merge.sourceBranch.name} />
          <StyledArrow width={18} height={18} />
          <BranchNameChip name={merge.targetBranch.name} />
        </BranchesSection>
      </Box>
      <StatsSection>
        {stats.map((stat) => (
          <StatCard key={stat.key}>
            <Typography
              variant="subtitle1"
              sx={{
                fontWeight:
                  stat.key === 'conflicts' && stat.value > 0 ? 600 : 500,
              }}
            >
              {stat.value}
            </Typography>
            <Typography
              variant="caption"
              sx={{ color: stat.color, textTransform: 'uppercase' }}
            >
              {stat.label}
            </Typography>
          </StatCard>
        ))}
      </StatsSection>
      <StatusSection>
        <BranchMergeStatus ready={readyToMerge} />
        {onDelete && (
          <Tooltip title={t('branch_merges_delete')}>
            <span>
              <IconButton
                size="small"
                onClick={onDelete}
                disabled={deleting}
                data-cy="project-branch-merge-delete"
              >
                <Trash01 width={18} height={18} />
              </IconButton>
            </span>
          </Tooltip>
        )}
      </StatusSection>
    </RowCard>
  );
};
