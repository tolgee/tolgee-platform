export const splitName = (key: string) => {
  const i = key.lastIndexOf('.');
  return i === -1
    ? { group: '', label: key }
    : { group: key.slice(0, i), label: key.slice(i + 1) };
};

const isColor = (v: unknown): v is string =>
  typeof v === 'string' && /^(#|rgb|hsl)/.test(v.trim());

export const flatten = (
  value: unknown,
  prefix = '',
): Record<string, string> => {
  if (isColor(value)) return { [prefix]: value };
  if (!value || typeof value !== 'object') return {};
  return Object.entries(value).reduce<Record<string, string>>(
    (acc, [k, v]) =>
      Object.assign(acc, flatten(v, prefix ? `${prefix}.${k}` : k)),
    {},
  );
};

export const byName = (a: string, b: string) =>
  a.localeCompare(b, 'en', { numeric: true, sensitivity: 'base' });

const BASE = ['main', 'dark', 'light', 'contrast', 'contrastText'];
const EMPHASIS = ['primary', 'secondary', 'tertiary', 'disabled'];
const STATES = [
  'disabledBackground',
  'hover',
  'selected',
  'focus',
  'focusVisible',
  'active',
];
const BORDERS = ['outlinedBorder', 'enabledBorder', 'hoverBorder'];
const SURFACES = [
  'background',
  'backgroundDark',
  'backgroundDarkHover',
  'fill',
  'color',
  'onDark',
  'onDarkHover',
];

const LABEL_ORDER = [
  ...BASE,
  ...EMPHASIS,
  ...STATES,
  ...BORDERS,
  ...SURFACES,
].map((n) => n.toLowerCase());

const rankOf = (label: string) => {
  const i = LABEL_ORDER.indexOf(label.toLowerCase());
  return i === -1 ? LABEL_ORDER.length : i;
};

const byLabel = (a: string, b: string) => {
  const la = splitName(a).label;
  const lb = splitName(b).label;
  const diff = rankOf(la) - rankOf(lb);
  return diff !== 0 ? diff : byName(la, lb);
};

export const groupByParent = (keys: string[]) => {
  const groups = new Map<string, string[]>();
  keys.forEach((k) => {
    const { group } = splitName(k);
    const existing = groups.get(group);
    if (existing) existing.push(k);
    else groups.set(group, [k]);
  });
  groups.forEach((groupKeys) => groupKeys.sort(byLabel));
  return groups;
};
