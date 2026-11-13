export type MentionTarget = { id: number; name: string };

export const mentionsToTokens = (text: string, picks: MentionTarget[]) => {
  if (!picks.length) {
    return text;
  }
  const idsByName = groupIdsByName(picks);
  const pattern = mentionPattern([...idsByName.keys()]);
  return text.replace(
    pattern,
    (_, name: string) => `<@${takeNextId(idsByName.get(name)!)}>`
  );
};

const groupIdsByName = (picks: MentionTarget[]) => {
  const result = new Map<string, number[]>();
  picks.forEach(({ id, name }) => {
    result.set(name, [...(result.get(name) ?? []), id]);
  });
  return result;
};

const mentionPattern = (names: string[]) => {
  const longestFirst = [...names].sort((a, b) => b.length - a.length);
  return new RegExp(
    `@(${longestFirst.map(escapeRegex).join('|')})(?![\\p{L}\\p{N}_])`,
    'gu'
  );
};

const takeNextId = (ids: number[]) => (ids.length > 1 ? ids.shift()! : ids[0]);

const escapeRegex = (value: string) =>
  value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
