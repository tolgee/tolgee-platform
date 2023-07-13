import * as Sentry from '@sentry/browser';

export function getUtmCookie() {
  return getCookie('tolgee_utm');
}

export function getUtmParams() {
  const cookie = getUtmCookie();
  if (!cookie) {
    return {};
  }
  try {
    return JSON.parse(atob(decodeURIComponent(cookie)));
  } catch (e) {
    Sentry.captureException(e);
  }
}

function getCookie(name) {
  const cookie = {};
  document.cookie.split(';').forEach(function (el) {
    const [k, v] = el.split('=');
    cookie[k.trim()] = v;
  });
  return cookie[name];
}
