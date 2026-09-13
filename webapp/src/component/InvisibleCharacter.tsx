import React from 'react';
import { styled, Tooltip } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { findInvisibleCharacters, InvisibleChar } from '@tginternal/editor';

const StyledNonBreakingSpace = styled('span')`
  background-color: ${({ theme }) => theme.palette.label.lightBlue};
  border-radius: 2px;
`;

const StyledZeroWidthBar = styled('span')`
  display: inline-block;
  width: 2px;
  height: 1em;
  background-color: ${({ theme }) => theme.palette.label.orange};
  vertical-align: text-bottom;
  border-radius: 1px;
`;

export function useInvisibleCharacterLabel() {
  const { t } = useTranslate();
  return (char: InvisibleChar): string => {
    switch (char.value) {
      case '\u00A0':
        return t(
          'invisible_character_no_break_space',
          'No-break space (U+00A0)'
        );
      case '\u202F':
        return t(
          'invisible_character_narrow_no_break_space',
          'Narrow no-break space (U+202F)'
        );
      case '\u2007':
        return t('invisible_character_figure_space', 'Figure space (U+2007)');
      case '\u200B':
        return t(
          'invisible_character_zero_width_space',
          'Zero-width space (U+200B)'
        );
      case '\uFEFF':
        return t(
          'invisible_character_byte_order_mark',
          'Zero-width no-break space, BOM (U+FEFF)'
        );
      case '\u00AD':
        return t('invisible_character_soft_hyphen', 'Soft hyphen (U+00AD)');
      default:
        return t('invisible_character_unknown', 'Invisible character');
    }
  };
}

type Props = {
  char: InvisibleChar;
};

export const InvisibleCharacter: React.FC<Props> = ({ char }) => {
  const getLabel = useInvisibleCharacterLabel();

  return (
    <Tooltip
      placement="bottom-start"
      enterDelay={200}
      leaveDelay={200}
      title={getLabel(char)}
    >
      {char.kind === 'zeroWidth' ? (
        <span data-cy="invisible-character" data-cy-kind={char.kind}>
          {char.value}
          <StyledZeroWidthBar />
        </span>
      ) : (
        <StyledNonBreakingSpace
          data-cy="invisible-character"
          data-cy-kind={char.kind}
        >
          {char.value}
        </StyledNonBreakingSpace>
      )}
    </Tooltip>
  );
};

export function renderWithInvisibleCharacters(
  text: string,
  keyPrefix: string
): React.ReactNode[] {
  const found = findInvisibleCharacters(text);
  if (found.length === 0) {
    return [text];
  }

  const nodes: React.ReactNode[] = [];
  let index = 0;
  found.forEach(({ index: at, char }) => {
    if (at > index) {
      nodes.push(text.substring(index, at));
    }
    nodes.push(<InvisibleCharacter key={`${keyPrefix}-${at}`} char={char} />);
    index = at + char.value.length;
  });
  if (index < text.length) {
    nodes.push(text.substring(index));
  }
  return nodes;
}
