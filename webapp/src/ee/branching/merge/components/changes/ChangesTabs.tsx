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

export const ChangesTabs = ({ tabs, selectedTab, onSelect }: Props) => (
  <Tabs
    value={selectedTab}
    onChange={(_, value) => onSelect(value as BranchMergeChangeType)}
    variant="scrollable"
    scrollButtons="auto"
    sx={{ borderBottom: 1, borderColor: 'divider' }}
  >
    {tabs.map((tab) => (
      <Tab
        key={tab.key}
        value={tab.key}
        disabled={tab.count === 0}
        label={
          <Box display="flex" gap={1} alignItems="center">
            <Typography variant="button">{tab.label}</Typography>
            <StyledBadge
              size="small"
              label={<Typography variant="body2">{tab.count}</Typography>}
            ></StyledBadge>
          </Box>
        }
      />
    ))}
  </Tabs>
);
