import {
  NO_CHOICE,
  ProjectChoice,
  chosenProjectId,
  isChoiceComplete,
} from 'tg.component/security/oauth2/consentProjectChoice';

const project = { id: 7, name: 'Demo' };

describe('isChoiceComplete', () => {
  it('accepts "all projects"', () => {
    expect(isChoiceComplete({ kind: 'all' as const })).toBe(true);
  });

  it('accepts a single project once one is picked', () => {
    expect(isChoiceComplete({ kind: 'one', project })).toBe(true);
  });

  // Approving here would have to guess what the user meant, and every guess is wider than what they asked for.
  it('rejects "a single project" with none picked', () => {
    expect(isChoiceComplete({ kind: 'one', project: null })).toBe(false);
  });
});

describe('NO_CHOICE', () => {
  // The widest grant must be asked for, so the screen starts on a state that cannot be approved.
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

  // Never falls through to "all projects": the approve button is disabled in this state instead.
  it('does not narrow when a single project is chosen but not picked', () => {
    const choice: ProjectChoice = { kind: 'one', project: null };
    expect(chosenProjectId(choice)).toBeUndefined();
  });
});
