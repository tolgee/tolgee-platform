import {
  NO_CHOICE,
  ProjectChoice,
  chosenProjectId,
  initialProjectChoice,
  isChoiceComplete,
} from './consentProjectChoice';

const project = { id: 7, name: 'Demo' };

describe('isChoiceComplete', () => {
  it('accepts "all projects"', () => {
    expect(isChoiceComplete({ kind: 'all' as const })).toBe(true);
  });

  it('accepts a single project once one is picked', () => {
    expect(isChoiceComplete({ kind: 'one', project })).toBe(true);
  });

  it('rejects "a single project" with none picked', () => {
    expect(isChoiceComplete({ kind: 'one', project: null })).toBe(false);
  });
});

describe('NO_CHOICE', () => {
  it('is not a complete choice', () => {
    expect(isChoiceComplete(NO_CHOICE)).toBe(false);
  });

  it('narrows nothing and names no project', () => {
    expect(chosenProjectId(NO_CHOICE)).toBeUndefined();
  });
});

describe('chosenProjectId', () => {
  it('does not narrow for "all projects"', () => {
    expect(chosenProjectId({ kind: 'all' as const })).toBeUndefined();
  });

  it('narrows to the picked project', () => {
    expect(chosenProjectId({ kind: 'one', project })).toBe(7);
  });

  it('does not narrow when a single project is chosen but not picked', () => {
    const choice: ProjectChoice = { kind: 'one', project: null };
    expect(chosenProjectId(choice)).toBeUndefined();
  });
});

describe('initialProjectChoice', () => {
  it('preselects a resolved project hint', () => {
    expect(initialProjectChoice({ project })).toEqual({
      kind: 'one',
      project,
    });
  });

  it('starts on no choice when the hint did not resolve', () => {
    expect(initialProjectChoice({ project: null })).toEqual(NO_CHOICE);
  });

  it('starts on no choice when the client named no project', () => {
    expect(initialProjectChoice({})).toEqual(NO_CHOICE);
  });
});
