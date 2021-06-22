/**
 * Returns all elements containing text without case matching
 * @param text the text to match
 * @param tag Tag containing the text
 * @param nth Number of the tag, if there are more then one
 * @param allowWrapped Whether there could be other element in the path
 * @returns {string}
 */
export const getAnyContainingText = (
  text,
  tag = '*',
  nth = 1,
  allowWrapped = true
) =>
  `//${tag}${
    allowWrapped ? '/descendant-or-self::*' : ''
  }/text()[contains(translate(.,` +
  `'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'), '${text.toLowerCase()}')][${nth}]/parent::*`;

export const getAnyContainingAriaLabelAttribute = (
  text,
  tag = '*',
  nth = 1,
  allowWrapped = true
) =>
  `//${tag}${allowWrapped ? '//*' : ''}[contains(translate(@aria-label,` +
  `'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'), '${text.toLowerCase()}')][${nth}]`;

export const getInput = (name, nth = 1) =>
  `//input[translate(@name,` +
  `'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz') = '${name.toLowerCase()}'][${nth}]`;

export const getClosestContainingText = (text, tag = '*', nth = 1) =>
  `./ancestor::*[.//*[${containsIgnoreCase(
    'text()',
    text
  )}]][1]//*[contains(text(), "${text}")]`;

export const containsIgnoreCase = (
  pathElementToContain: string,
  toBeContained: string
) => {
  return `contains(translate(${pathElementToContain}, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'), '${toBeContained.toLowerCase()}')`;
};
