import { isRequestedProjectInaccessible } from './consentProjectAccess';

describe('isRequestedProjectInaccessible', () => {
  it('is false when the client asked for nothing', () => {
    expect(isRequestedProjectInaccessible({})).toBe(false);
  });

  it('is false when the requested project resolved', () => {
    expect(
      isRequestedProjectInaccessible({
        project: { id: 7, name: 'Demo' },
        requestedProjectId: 7,
      })
    ).toBe(false);
  });

  it('is true when the requested project did not resolve', () => {
    expect(
      isRequestedProjectInaccessible({ project: null, requestedProjectId: 42 })
    ).toBe(true);
  });
});
