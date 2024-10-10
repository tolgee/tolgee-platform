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
    <T keyName="activity_translation_outdated" />
  ) : (
    <T keyName="activity_translation_not_outdated" />
  );
};

export const getOutdatedChange = (input?: DiffValue<boolean>) => {
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
