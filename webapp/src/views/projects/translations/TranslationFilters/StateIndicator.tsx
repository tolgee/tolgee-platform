import { styled } from '@mui/material';
import { Flag02 } from '@untitled-ui/icons-react';
import { TRANSLATION_STATES } from 'tg.constants/translationStates';
import { Mt } from 'tg.component/CustomIcons';
import { TranslationStateType } from './tools';

const StyledDot = styled('div')`
  margin-left: 12px;
  margin-right: 4px;
  width: 8px;
  height: 8px;
  border-radius: 4px;
`;

const StyledIcon = styled('div')`
  margin-left: 8px;
  display: flex;
  align-items: center;
  font-size: 16px;
  width: 16px;
  height: 16px;
  color: ${({ theme }) => theme.palette.tokens.icon.secondary};
`;

type Props = {
  state: TranslationStateType;
};

export const StateIndicator = ({ state }: Props) => {
  if (state === 'AUTO_TRANSLATED' || state === 'OUTDATED') {
    return (
      <StyledIcon>
        {state === 'AUTO_TRANSLATED' && <Mt />}
        {state === 'OUTDATED' && <Flag02 />}
      </StyledIcon>
    );
  }

  const color = TRANSLATION_STATES[state].color;
  return <StyledDot style={{ background: color }} />;
};
