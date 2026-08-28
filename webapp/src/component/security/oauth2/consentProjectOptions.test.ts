import { describe, expect, it } from 'vitest';
import { deriveConsentProjects } from 'tg.component/security/oauth2/consentProjectOptions';

describe('deriveConsentProjects', () => {
  it('no hint: not inaccessible, no per-project option', () => {
    expect(deriveConsentProjects({})).toEqual({
      requestedInaccessible: false,
      projectOptions: [],
    });
  });

  it('accessible hint: offers only the declared project', () => {
    const project = { id: 7, name: 'Demo' };
    expect(deriveConsentProjects({ project, requestedProjectId: 7 })).toEqual({
      requestedInaccessible: false,
      projectOptions: [project],
    });
  });

  it('requested but inaccessible: flagged, no project offered', () => {
    expect(
      deriveConsentProjects({ project: null, requestedProjectId: 42 })
    ).toEqual({ requestedInaccessible: true, projectOptions: [] });
  });
});
