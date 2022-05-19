import { styled } from '@mui/material';
import { CircledLanguageIcon } from 'tg.component/languages/CircledLanguageIcon';
import { DiffValue } from '../types';

const StyledCircledLanguageIcon = styled(CircledLanguageIcon)`
  display: inline-block;
  vertical-align: text-bottom;
`;

const StyledDiff = styled('span')`
  word-break: break-word;
`;

const StyledRemoved = styled('span')`
  text-decoration: line-through;
`;

const StyledArrow = styled('span')`
  padding: 0px 6px;
`;

type Language = {
  tag: string;
  name: string;
  flagEmoji: string;
};

const formatLanguage = (lang: Language) => {
  return (
    <>
      <StyledCircledLanguageIcon size={14} flag={lang.flagEmoji} />
      {` ${lang.name}(${lang.tag})`}
    </>
  );
};

export const getProjectLanguageChange = (
  input?: DiffValue<{ data: Language }>
) => {
  const oldInput = input?.old;
  const newInput = input?.new;
  if (oldInput && newInput) {
    return (
      <StyledDiff>
        <StyledRemoved>{formatLanguage(oldInput.data)}</StyledRemoved>
        <StyledArrow>→</StyledArrow>
        <span>{formatLanguage(newInput.data)}</span>
      </StyledDiff>
    );
  } else if (oldInput) {
    return (
      <StyledDiff>
        <StyledRemoved>{formatLanguage(oldInput.data)}</StyledRemoved>
      </StyledDiff>
    );
  } else if (newInput) {
    return (
      <StyledDiff>
        <span>{formatLanguage(newInput.data)}</span>
      </StyledDiff>
    );
  }
};
