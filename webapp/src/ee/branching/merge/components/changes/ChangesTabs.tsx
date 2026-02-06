import { Box, Chip, styled, Tab, Tabs, Typography } from '@mui/material';
import { BranchMergeChangeType } from '../../types';

type TabConfig = {
  key: BranchMergeChangeType;
  label: string;
  count: number;
};

type Props = {
  tabs: TabConfig[];
  selectedTab: BranchMergeChangeType;
  onSelect: (tab: BranchMergeChangeType) => void;
};

const StyledBadge = styled(Chip)(() => ({}));

export const ChangesTabs = ({ tabs, selectedTab, onSelect }: Props) => {
  const getTab = (key: BranchMergeChangeType) =>
    tabs.find((t) => t.key === key);

  const addTab = getTab('ADD');
  const updateTab = getTab('UPDATE');
  const deleteTab = getTab('DELETE');
  const conflictTab = getTab('CONFLICT');

  return (
    <Tabs
      value={selectedTab}
      onChange={(_, value) => onSelect(value as BranchMergeChangeType)}
      variant="scrollable"
      scrollButtons="auto"
      sx={{ borderBottom: 1, borderColor: 'divider' }}
      data-cy="merge-changes-tabs"
    >
      {addTab && (
        <Tab
          value="ADD"
          disabled={addTab.count === 0}
          data-cy="merge-tab-add"
          label={
            <Box display="flex" gap={1} alignItems="center">
              <Typography variant="button">{addTab.label}</Typography>
              <StyledBadge
                size="small"
                label={
                  <Typography variant="body2" data-cy="merge-tab-add-count">
                    {addTab.count}
                  </Typography>
                }
              />
            </Box>
          }
        />
      )}
      {updateTab && (
        <Tab
          value="UPDATE"
          disabled={updateTab.count === 0}
          data-cy="merge-tab-update"
          label={
            <Box display="flex" gap={1} alignItems="center">
              <Typography variant="button">{updateTab.label}</Typography>
              <StyledBadge
                size="small"
                label={
                  <Typography variant="body2" data-cy="merge-tab-update-count">
                    {updateTab.count}
                  </Typography>
                }
              />
            </Box>
          }
        />
      )}
      {deleteTab && (
        <Tab
          value="DELETE"
          disabled={deleteTab.count === 0}
          data-cy="merge-tab-delete"
          label={
            <Box display="flex" gap={1} alignItems="center">
              <Typography variant="button">{deleteTab.label}</Typography>
              <StyledBadge
                size="small"
                label={
                  <Typography variant="body2" data-cy="merge-tab-delete-count">
                    {deleteTab.count}
                  </Typography>
                }
              />
            </Box>
          }
        />
      )}
      {conflictTab && (
        <Tab
          value="CONFLICT"
          disabled={conflictTab.count === 0}
          data-cy="merge-tab-conflict"
          label={
            <Box display="flex" gap={1} alignItems="center">
              <Typography variant="button">{conflictTab.label}</Typography>
              <StyledBadge
                size="small"
                label={
                  <Typography
                    variant="body2"
                    data-cy="merge-tab-conflict-count"
                  >
                    {conflictTab.count}
                  </Typography>
                }
              />
            </Box>
          }
        />
      )}
    </Tabs>
  );
};
