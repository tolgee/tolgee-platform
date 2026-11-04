import { FC, ReactNode } from 'react';
import { Box, FormControlLabel, Switch, Typography } from '@mui/material';
import { T } from '@tolgee/react';
import {
  BranchMergeFilters,
  BranchMergeModel,
} from 'tg.ee.module/branching/merge/types';

type Props = {
  merge: BranchMergeModel;
  onChange: (filters: BranchMergeFilters) => void;
  disabled: boolean;
};

const FilterLabel: FC<{ label: ReactNode; hint: ReactNode }> = ({
  label,
  hint,
}) => (
  <Box display="flex" flexDirection="column">
    <span>{label}</span>
    <Typography variant="caption" color="textSecondary">
      {hint}
    </Typography>
  </Box>
);

export const MergeFilters: FC<Props> = ({ merge, onChange, disabled }) => (
  <Box display="flex" gap={3} flexWrap="wrap">
    <FormControlLabel
      control={
        <Switch
          data-cy="project-branch-merge-filter-reviewed-only"
          checked={merge.filters.reviewedOnly}
          disabled={disabled}
          onChange={(e) =>
            onChange({ ...merge.filters, reviewedOnly: e.target.checked })
          }
        />
      }
      label={
        <FilterLabel
          label={
            <T
              keyName="branch_merge_filter_reviewed_only"
              defaultValue="Reviewed only"
            />
          }
          hint={
            <T
              keyName="branch_merge_filter_reviewed_only_hint"
              defaultValue="Skips keys where any translation in <b>{source}</b> is not reviewed yet."
              params={{ source: merge.sourceBranchName, b: <b /> }}
            />
          }
        />
      }
    />
    {!merge.isUpdate && (
      <FormControlLabel
        control={
          <Switch
            data-cy="project-branch-merge-filter-existing-keys-only"
            checked={merge.filters.existingKeysOnly}
            disabled={disabled}
            onChange={(e) =>
              onChange({ ...merge.filters, existingKeysOnly: e.target.checked })
            }
          />
        }
        label={
          <FilterLabel
            label={
              <T
                keyName="branch_merge_filter_existing_keys_only"
                defaultValue="Existing keys only"
              />
            }
            hint={
              <T
                keyName="branch_merge_filter_existing_keys_only_hint"
                defaultValue="Skips keys that do not exist in <b>{target}</b> yet."
                params={{ target: merge.targetBranchName, b: <b /> }}
              />
            }
          />
        }
      />
    )}
  </Box>
);
