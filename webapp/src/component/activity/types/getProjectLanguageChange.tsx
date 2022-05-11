import { styled } from '@mui/material';
import { CircledLanguageIcon } from 'tg.component/languages/CircledLanguageIcon';
import { DiffValue } from '../types';

const StyledCircledLanguageIcon = styled(CircledLanguageIcon)`
  display: inline-block;
  vertical-align: text-bottom;
`;

const StyledDiff = styled('span')`
  word-break: break-word;
  & .removed {
    text-decoration: line-through;
  }
  & .added {
  }
  & .arrow {
    padding: 0px 6px;
  }
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
        <span className="removed">{formatLanguage(oldInput.data)}</span>
        <span className="arrow">→</span>
        <span className="added">{formatLanguage(newInput.data)}</span>
      </StyledDiff>
    );
  } else if (oldInput) {
    return (
      <StyledDiff>
        <span className="removed">{formatLanguage(oldInput.data)}</span>
      </StyledDiff>
    );
  } else if (newInput) {
    return (
      <StyledDiff>
        <span className="added">{formatLanguage(newInput.data)}</span>
      </StyledDiff>
    );
  }
};
