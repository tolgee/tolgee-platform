// The `continue` target is written by the backend and always points at its own /oauth2/authorize.
// We still validate it before navigating, so a crafted ?continue=... can't turn this into an open redirect.
export const isSafeContinue = (
  url: string | undefined,
  apiUrl: string
): url is string => {
  if (!url) {
    return false;
  }
  try {
    const target = new URL(url);
    const baseOrigin = apiUrl ? new URL(apiUrl).origin : window.location.origin;
    return (
      target.origin === baseOrigin && target.pathname === '/oauth2/authorize'
    );
  } catch {
    return false;
  }
};
