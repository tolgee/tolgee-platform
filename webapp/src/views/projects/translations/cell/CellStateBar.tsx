import { styled, Tooltip } from '@mui/material';

import { TRANSLATION_STATES } from 'tg.constants/translationStates';
import { stopAndPrevent } from 'tg.fixtures/eventHandler';
import { components } from 'tg.service/apiSchema.generated';

type State = components['schemas']['TranslationViewModel']['state'];

const StyledStateHover = styled('div')`
  position: absolute;
  width: 12px;
  height: 100%;
  z-index: 1;
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
            TRANSLATION_STATES[state || 'UNTRANSLATED']?.color ||
            TRANSLATION_STATES['UNTRANSLATED'].color
          }`,
        }}
      />
    </StyledStateHover>
  );

  return state && TRANSLATION_STATES[state] ? (
    <Tooltip title={TRANSLATION_STATES[state].translation}>
      {getContent()}
    </Tooltip>
  ) : (
    getContent()
  );
};
