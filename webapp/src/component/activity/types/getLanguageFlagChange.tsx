import { styled } from '@mui/material';
import { InlineLanguageIcon } from 'tg.component/languages/InlineLanguageIcon';
import { DiffValue } from '../types';

const StyledDiff = styled('span')`
  word-break: break-word;
`;

const StyledRemoved = styled('span')`
  position: relative;
  &::after {
    content: '';
    pointer-events: none;
    position: absolute;
    top: 50%;
    left: 0;
    right: 0;
    background: ${({ theme }) => theme.palette.text.primary};
    height: 1px;
  }
`;

const StyledArrow = styled('span')`
  padding: 0px 6px;
`;

export const getLanguageFlagChange = (input?: DiffValue<string>) => {
  const oldInput = input?.old;
  const newInput = input?.new;
  if (oldInput && newInput) {
    return (
      <StyledDiff>
        <InlineLanguageIcon flag={oldInput} />
        <StyledArrow>→</StyledArrow>
        <InlineLanguageIcon flag={newInput} />
      </StyledDiff>
    );
  } else if (oldInput) {
    return (
      <StyledDiff>
        <StyledRemoved>
          <InlineLanguageIcon flag={oldInput} />
        </StyledRemoved>
      </StyledDiff>
    );
  } else if (newInput) {
    return (
      <StyledDiff>
        <span>
          <InlineLanguageIcon flag={newInput} />
        </span>
      </StyledDiff>
    );
  }
};
