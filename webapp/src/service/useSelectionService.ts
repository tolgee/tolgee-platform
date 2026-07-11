import { useEffect, useState } from 'react';

export type SelectionServiceProps<T> = {
  initialSelected?: T[];
  totalCount?: number;
  itemsInRange?: (to: T, from: T) => Promise<T[]>;
  itemsAll?: () => Promise<T[]>;
  onChanged?: (selected: T[]) => void;
};

export type SelectionService<T> = {
  selected: T[];
  total: number;
  reset: () => void;
  toggle: (item: T) => void;
  select: (item: T) => void;
  unselect: (item: T) => void;
  toggleSelectAll: () => Promise<void>;
  toggleAll: () => Promise<void>;
  selectAll: () => Promise<void>;
  unselectAll: () => void;
  toggleMultiple: (items: T[]) => void;
  selectMultiple: (items: T[]) => void;
  unselectMultiple: (items: T[]) => void;
  toggleRange: (to: T, from?: T) => Promise<void>;
  selectRange: (to: T, from?: T) => Promise<void>;
  unselectRange: (to: T, from?: T) => Promise<void>;
  isSelected: (item: T) => boolean;
  isAllSelected: boolean;
  isSomeSelected: boolean;
  isAnySelected: boolean;
  isNoneSelected: boolean;
  isLoading: boolean;
};

export function useSelectionService<T>(
  props?: SelectionServiceProps<T>
): SelectionService<T> {
  const {
    initialSelected = [],
    totalCount = 0,
    itemsInRange = async (to: T) => [to],
    itemsAll = async () => [],
    onChanged = () => {},
  } = props || {};

  const [selected, setSelectedField] = useState<T[]>(initialSelected);
  const [loadingCnt, setLoadingCnt] = useState(0);
  const [lastSelected, setLastSelected] = useState<T | null>(null);
  const [warningPrinted, setWarningPrinted] = useState(false);

  const setSelected = (selected: T[]) => {
    setSelectedField(selected);
    onChanged(selected);
  };

  const resolveRange = async (to: T, from?: T) => {
    setLastSelected(to);
    setLoadingCnt((c) => c + 1);
    const items = await itemsInRange(to, from || lastSelected || to);
    setLoadingCnt((c) => c - 1);
    return items;
  };

  const resolveAll = async () => {
    setLoadingCnt((c) => c + 1);
    const items = await itemsAll();
    setLoadingCnt((c) => c - 1);
    return items;
  };

  const clearLastSelected = () => {
    setLastSelected(null);
  };

  const selectUnsafe = (item: T) => {
    setSelected([...selected, item]);
    setLastSelected(item);
  };

  const unselect = (item: T) => {
    setSelected(selected.filter((i) => i !== item));
    setLastSelected(item);
  };

  const selectAll = async () => {
    setSelected(await resolveAll());
    clearLastSelected();
  };

  const unselectAll = () => {
    setSelected([]);
    clearLastSelected();
  };

  const toggleMultiple = (items: T[]) => {
    const missing = items.filter((i) => !selected.includes(i));
    setSelected([...selected.filter((i) => !items.includes(i)), ...missing]);
  };

  const selectMultiple = (items: T[]) => {
    const missing = items.filter((i) => !selected.includes(i));
    setSelected([...selected, ...missing]);
  };

  const unselectMultiple = (items: T[]) => {
    setSelected(selected.filter((i) => !items.includes(i)));
  };

  useEffect(() => {
    if (!warningPrinted && selected.length > totalCount) {
      // eslint-disable-next-line no-console
      console.warn(
        `Selected count ${selected.length} is greater than total count ${totalCount}. This is probably a bug.`
      );
      setWarningPrinted(true);
    }
  }, [totalCount, selected.length]);

  const isAllSelected = selected.length >= totalCount;
  const isSomeSelected = selected.length > 0 && selected.length < totalCount;
  const isAnySelected = selected.length > 0;
  const isNoneSelected = selected.length === 0;
  const isLoading = loadingCnt > 0;

  return {
    selected: selected,
    total: totalCount,
    reset: () => {
      setSelected(initialSelected);
    },
    toggle: (item: T) => {
      if (selected.includes(item)) {
        unselect(item);
      } else {
        selectUnsafe(item);
      }
    },
    select: (item: T) => {
      if (selected.includes(item)) {
        return;
      }
      selectUnsafe(item);
    },
    unselect,
    toggleSelectAll: async () => {
      if (isAllSelected) {
        unselectAll();
      } else {
        await selectAll();
      }
      setLastSelected(null);
    },
    toggleAll: async () => {
      toggleMultiple(await resolveAll());
    },
    selectAll,
    unselectAll,
    toggleMultiple,
    selectMultiple,
    unselectMultiple,
    toggleRange: async (to: T, from?: T) => {
      toggleMultiple(await resolveRange(to, from));
    },
    selectRange: async (to: T, from?: T) => {
      selectMultiple(await resolveRange(to, from));
    },
    unselectRange: async (to: T, from?: T) => {
      unselectMultiple(await resolveRange(to, from));
    },
    isSelected: (item: T) => selected.includes(item),
    isAllSelected,
    isSomeSelected,
    isAnySelected,
    isNoneSelected,
    isLoading,
  };
}
