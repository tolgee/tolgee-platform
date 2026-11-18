import { act, StrictMode } from 'react';
import { createRoot } from 'react-dom/client';

import { RedirectSignedUser } from './RedirectSignedUser';

const redirectAfterLogin = vi.fn();
let allowPrivate = false;

vi.mock('tg.globalContext/GlobalContext', () => ({
  useGlobalContext: (selector: (c: unknown) => unknown) =>
    selector({ auth: { allowPrivate } }),
  useGlobalActions: () => ({ redirectAfterLogin }),
}));

vi.mock('tg.component/GlobalLoading', () => ({
  GlobalLoading: () => null,
}));

const render = () => {
  const root = createRoot(document.createElement('div'));
  act(() => {
    root.render(
      <StrictMode>
        <RedirectSignedUser>content</RedirectSignedUser>
      </StrictMode>
    );
  });
  return root;
};

describe('RedirectSignedUser', () => {
  beforeAll(() => {
    (globalThis as any).IS_REACT_ACT_ENVIRONMENT = true;
  });

  beforeEach(() => {
    redirectAfterLogin.mockClear();
  });

  it('redirects once when mounted signed in under StrictMode', () => {
    allowPrivate = true;
    render();
    expect(redirectAfterLogin).toHaveBeenCalledTimes(1);
  });

  it('does not redirect a signed-out user', () => {
    allowPrivate = false;
    render();
    expect(redirectAfterLogin).not.toHaveBeenCalled();
  });
});
