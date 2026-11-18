import { styled } from '@mui/material';
import { T } from '@tolgee/react';
import { DiffValue } from '../types';

const StyledState = styled('div')`
  display: flex;
  align-items: center;
  gap: 6px;
`;

const getTranslation = (value: boolean | undefined) => {
  return value ? (
    <T keyName="activity_ai_review_on" defaultValue="On" />
  ) : (
    <T keyName="activity_ai_review_off" defaultValue="Off" />
  );
};

export const getAiReviewEnabled = (input?: DiffValue<boolean>) => {
  if (input?.new !== undefined) {
    return (
      <StyledState>
        {getTranslation(input.old)}
        <div>→</div>
        {getTranslation(input.new)}
      </StyledState>
    );
  }
};
