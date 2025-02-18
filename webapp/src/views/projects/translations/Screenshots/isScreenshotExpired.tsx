export function isScreenshotExpired(src: string | undefined): boolean {
  if (!src) {
    return false;
  }
  const expiration = getExpirationFromSrc(src);
  if (!expiration) {
    return false;
  }
  const now = Date.now();
  return now > expiration * 1000;
}

export function getExpirationFromSrc(src: string): undefined | number {
  const link = new URL(src);
  const tokenPart = link.searchParams.get('token')?.split('.')[1];
  if (tokenPart) {
    return JSON.parse(atob(tokenPart))?.exp as number | undefined;
  }
}
