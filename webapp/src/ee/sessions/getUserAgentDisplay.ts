export type UserAgentDisplay = {
  label: string;
  title?: string;
};

const RAW_MAX_LENGTH = 40;

// Order matters, the patterns overlap: Edge and Opera user agents contain
// `Chrome`, and Chrome user agents contain `Safari`.
const BROWSERS: [RegExp, string][] = [
  [/Edg[A-Za-z]*\//, 'Edge'],
  [/OPR\/|OPiOS\/|Opera/, 'Opera'],
  [/SamsungBrowser\//, 'Samsung Internet'],
  [/Firefox\/|FxiOS\//, 'Firefox'],
  [/Chrome\/|CriOS\/|Chromium\//, 'Chrome'],
  [/Safari\//, 'Safari'],
];

// Order matters, the patterns overlap: Android user agents contain `Linux`,
// ChromeOS user agents contain `X11` and iOS user agents contain `like Mac OS X`.
const OPERATING_SYSTEMS: [RegExp, string][] = [
  [/Windows/, 'Windows'],
  [/Android/, 'Android'],
  [/iPhone|iPad|iPod|iOS/, 'iOS'],
  [/CrOS/, 'ChromeOS'],
  [/Mac OS X|Macintosh/, 'macOS'],
  [/Linux|X11/, 'Linux'],
];

function findMatch(patterns: [RegExp, string][], userAgent: string) {
  return patterns.find(([pattern]) => pattern.test(userAgent))?.[1];
}

export function getUserAgentDisplay(userAgent: string): UserAgentDisplay {
  const browser = findMatch(BROWSERS, userAgent);
  const os = findMatch(OPERATING_SYSTEMS, userAgent);

  if (browser && os) {
    return { label: `${browser} · ${os}` };
  }

  if (browser || os) {
    return { label: (browser || os) as string };
  }

  const raw = userAgent.trim();
  return {
    label:
      raw.length > RAW_MAX_LENGTH ? `${raw.slice(0, RAW_MAX_LENGTH)}…` : raw,
    title: raw,
  };
}
