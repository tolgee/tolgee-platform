import { Project } from 'tg.component/projectSearchSelect/types';

export type ProjectChoice =
  | { kind: 'unset' }
  | { kind: 'all' }
  | { kind: 'one'; project: Project | null };

export const NO_CHOICE: ProjectChoice = { kind: 'unset' };

export const isChoiceComplete = (choice: ProjectChoice): boolean => {
  if (choice.kind === 'all') return true;
  if (choice.kind === 'one') return choice.project !== null;
  return false;
};

export const chosenProjectId = (choice: ProjectChoice): number | undefined =>
  choice.kind === 'one' && choice.project ? choice.project.id : undefined;

/**
 * What the picker starts on. A resolved `project` hint is pre-selected so the extension's own flow completes without
 * touching the picker; anything else has to be chosen, because no other value is one the user approved.
 */
export const initialProjectChoice = (info: {
  project?: { id: number; name: string } | null;
}): ProjectChoice =>
  info.project
    ? { kind: 'one', project: { id: info.project.id, name: info.project.name } }
    : NO_CHOICE;
