import { Project } from 'tg.component/projectSearchSelect/types';

export type ProjectChoice =
  | { kind: 'unset' }
  | { kind: 'all' }
  | { kind: 'one'; project: Project | null };

/** Nothing chosen yet — the widest grant has to be asked for, so it is never where the screen starts. */
export const NO_CHOICE: ProjectChoice = { kind: 'unset' };

export const isChoiceComplete = (choice: ProjectChoice): boolean => {
  if (choice.kind === 'all') return true;
  if (choice.kind === 'one') return choice.project !== null;
  return false;
};

export const chosenProjectId = (choice: ProjectChoice): number | undefined =>
  choice.kind === 'one' && choice.project ? choice.project.id : undefined;
