import { HighlightItem, ItemStep } from './enums';

export type Action = {
  link?: string;
  label: React.ReactNode;
  highlightItems?: HighlightItem[];
};

export type ItemType = {
  step: ItemStep;
  name: React.ReactNode;
  actions?: (data: { projectId: number | undefined }) => Action[];
  needsProject?: boolean;
};
