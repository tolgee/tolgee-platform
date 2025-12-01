import { useEffect, useMemo } from 'react';
import { BranchMergeChangeType, BranchMergeModel } from '../types';

type TabConfig = {
  key: BranchMergeChangeType;
  label: string;
  count: number;
};

export const useMergeTabs = (
  merge: BranchMergeModel | undefined,
  labels: Record<BranchMergeChangeType, string>,
  selectedTab: BranchMergeChangeType,
  setSelectedTab: (tab: BranchMergeChangeType) => void
) => {
  const totalConflicts = useMemo(
    () =>
      merge
        ? merge.keyResolvedConflictsCount + merge.keyUnresolvedConflictsCount
        : 0,
    [merge]
  );

  const tabs: TabConfig[] = useMemo(() => {
    if (!merge) return [];
    return [
      { key: 'ADD', label: labels.ADD, count: merge.keyAdditionsCount },
      {
        key: 'UPDATE',
        label: labels.UPDATE,
        count: merge.keyModificationsCount,
      },
      { key: 'DELETE', label: labels.DELETE, count: merge.keyDeletionsCount },
      { key: 'CONFLICT', label: labels.CONFLICT, count: totalConflicts },
    ];
  }, [merge, labels, totalConflicts]);

  useEffect(() => {
    if (!merge || !tabs.length) return;

    const conflictTab = tabs.find((t) => t.key === 'CONFLICT');
    const firstWithData =
      conflictTab && conflictTab.count > 0
        ? conflictTab
        : tabs.find((t) => t.count > 0) || tabs[0];

    if (!tabs.find((t) => t.key === selectedTab)) {
      setSelectedTab(firstWithData.key);
    }
  }, [merge, tabs, selectedTab, setSelectedTab]);

  return { tabs, totalConflicts };
};
