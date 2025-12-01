import { Tabs, Tab, Badge, styled, BadgeProps } from '@mui/material';
import { BranchMergeChangeType } from '../types';

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

const StyledBadge = styled(Badge)<BadgeProps>(() => ({
  '& .MuiBadge-badge': {
    right: -6,
  },
}));

export const ChangesTabs = ({ tabs, selectedTab, onSelect }: Props) => (
  <Tabs
    value={selectedTab}
    onChange={(_, value) => onSelect(value as BranchMergeChangeType)}
    variant="scrollable"
    scrollButtons="auto"
  >
    {tabs.map((tab) => (
      <Tab
        key={tab.key}
        value={tab.key}
        disabled={tab.count === 0}
        label={
          <StyledBadge
            color="primary"
            badgeContent={tab.count}
            max={999}
            anchorOrigin={{ vertical: 'top', horizontal: 'right' }}
          >
            {tab.label}
          </StyledBadge>
        }
      />
    ))}
  </Tabs>
);
