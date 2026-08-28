import { beforeEach, describe, expect, it, vi } from 'vitest';

import { apiV2HttpService } from 'tg.service/http/ApiV2HttpService';

import { ALL_PROJECTS, selectConsentProject } from './oauth2ConsentSubmit';

vi.mock('tg.service/http/ApiV2HttpService', () => ({
  apiV2HttpService: { post: vi.fn(() => Promise.resolve({})) },
}));

const post = vi.mocked(apiV2HttpService.post);

describe('selectConsentProject', () => {
  beforeEach(() => post.mockClear());

  it('names the chosen project, which is what narrows the token', () => {
    selectConsentProject({ state: 'the-state', selectedProject: 42 });

    expect(post).toHaveBeenCalledWith(
      'oauth2/select-project?state=the-state&projectId=42',
      {}
    );
  });

  it('sends no projectId for All projects, so the token is not narrowed', () => {
    selectConsentProject({ state: 'the-state', selectedProject: ALL_PROJECTS });

    expect(post).toHaveBeenCalledWith(
      'oauth2/select-project?state=the-state',
      {}
    );
  });

  it('encodes the state, which is opaque and can carry url-unsafe bytes', () => {
    selectConsentProject({ state: 'a/b+c=', selectedProject: 42 });

    expect(post).toHaveBeenCalledWith(
      'oauth2/select-project?state=a%2Fb%2Bc%3D&projectId=42',
      {}
    );
  });
});
