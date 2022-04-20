import { styled, Tooltip } from '@mui/material';
import { T } from '@tolgee/react';

import { translationStates } from 'tg.constants/translationStates';
import { stopAndPrevent } from 'tg.fixtures/eventHandler';
import { components } from 'tg.service/apiSchema.generated';

type State = components['schemas']['TranslationViewModel']['state'];

const StyledStateHover = styled('div')`
  position: absolute;
  width: 12px;
  height: 100%;
`;

const StyledState = styled('div')`
  cursor: col-resize;
  height: 100%;
  width: 4px;
  filter: brightness(
    ${({ theme }) => (theme.palette.mode === 'dark' ? 0.7 : 1)}
  );
`;

type Props = {
  state?: State;
  onResize: React.MouseEventHandler<HTMLDivElement>;
};

export const CellStateBar: React.FC<Props> = ({ state, onResize }) => {
  const getContent = () => (
    <StyledStateHover data-cy="translations-state-indicator">
      <StyledState
        onMouseDown={stopAndPrevent(onResize)}
        onClick={stopAndPrevent()}
        onMouseUp={stopAndPrevent()}
        style={{
          borderLeft: `4px solid ${
            translationStates[state || 'UNTRANSLATED']?.color ||
            translationStates['UNTRANSLATED'].color
          }`,
        }}
      />
    </StyledStateHover>
  );

  return state && translationStates[state] ? (
    <Tooltip title={<T noWrap>{translationStates[state].translationKey}</T>}>
      {getContent()}
    </Tooltip>
  ) : (
    getContent()
  );
};
