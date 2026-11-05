import { useEffect, useMemo } from 'react';
import { BranchMergeChangeType, BranchMergeModel } from '../types';

type TabConfig = {
  key: BranchMergeChangeType;
  label: string;
  count: number;
  excluded: number;
};

export const useMergeTabs = (
  merge: BranchMergeModel | undefined,
  labels: Record<BranchMergeChangeType, string>,
  selectedTab: BranchMergeChangeType,
  setSelectedTab: (tab: BranchMergeChangeType) => void,
  userSelected: boolean
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
    return (
      [
        {
          key: 'ADD',
          label: labels.ADD,
          count: merge.keyAdditionsCount,
          excluded: merge.keyExcludedAdditionsCount,
        },
        {
          key: 'UPDATE',
          label: labels.UPDATE,
          count: merge.keyModificationsCount,
          excluded: merge.keyExcludedModificationsCount,
        },
        {
          key: 'DELETE',
          label: labels.DELETE,
          count: merge.keyDeletionsCount,
          excluded: merge.keyExcludedDeletionsCount,
        },
        {
          key: 'CONFLICT',
          label: labels.CONFLICT,
          count: totalConflicts,
          excluded: merge.keyExcludedConflictsCount,
        },
      ] satisfies TabConfig[]
    ).filter((tab) => tab.count > 0 || tab.excluded > 0);
  }, [merge, labels, totalConflicts]);

  useEffect(() => {
    if (!tabs.length) return;
    if (userSelected && tabs.some((t) => t.key === selectedTab)) return;

    const conflictTab = tabs.find((t) => t.key === 'CONFLICT');
    const firstWithData =
      conflictTab && conflictTab.count > 0
        ? conflictTab
        : tabs.find((t) => t.count > 0) || tabs[0];

    if (firstWithData.key !== selectedTab) {
      setSelectedTab(firstWithData.key);
    }
  }, [tabs, selectedTab, setSelectedTab, userSelected]);

  return { tabs, totalConflicts };
};
