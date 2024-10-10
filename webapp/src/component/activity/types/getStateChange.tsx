import { styled } from '@mui/material';
import { StateType, TRANSLATION_STATES } from 'tg.constants/translationStates';
import { DiffValue } from '../types';

const StyledState = styled('div')`
  display: flex;
  align-items: center;
  gap: 6px;
`;

const StyleWrapper = styled('div')`
  display: flex;
  align-items: center;
  gap: 2px;
`;

const StyledDot = styled('div')`
  margin: 2px;
  grid-area: dot;
  width: 8px;
  height: 8px;
  border-radius: 50%;
`;

const getTranslation = (state: StateType) => {
  const translationState = TRANSLATION_STATES[state];

  return translationState ? (
    <StyleWrapper>
      <StyledDot style={{ background: translationState.color }} />
      <div>{translationState.translation}</div>
    </StyleWrapper>
  ) : (
    ''
  );
};

export const getStateChange = (input?: DiffValue) => {
  if (input?.new) {
    return (
      <StyledState>
        {getTranslation((input.old || 'UNTRANSLATED') as StateType)}
        <div>→</div>
        {getTranslation(input.new as StateType)}
      </StyledState>
    );
  }
};
