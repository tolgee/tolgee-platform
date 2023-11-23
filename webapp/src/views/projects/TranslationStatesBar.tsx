import { useEffect, useState } from 'react';
import clsx from 'clsx';
import { Box, styled, Tooltip } from '@mui/material';
import { T } from '@tolgee/react';

import { TRANSLATION_STATES } from 'tg.constants/translationStates';

type State = keyof typeof TRANSLATION_STATES;

const HEIGHT = 8;
const BORDER_RADIUS = HEIGHT / 2;
const DOT_SIZE = 8;

const StyledContainer = styled('div')`
  width: 100%;

  & .bar {
    height: ${HEIGHT}px;
    background-color: ${TRANSLATION_STATES.UNTRANSLATED.color};
    border-radius: ${BORDER_RADIUS}px;
    width: 100%;
    overflow: hidden;
    display: flex;
    margin-bottom: ${({ theme }) => theme.spacing(0.5)};
    filter: brightness(
      ${({ theme }) => (theme.palette.mode === 'dark' ? 0.8 : 1)}
    );
  }

  & .state {
    height: 100%;
    overflow: hidden;
    border-radius: ${BORDER_RADIUS}px;
    margin-left: ${-BORDER_RADIUS}px;
    transition: width 0.4s ease-in-out;
  }

  & .loadedState {
    width: 0px !important;
  }

  & .legend {
    display: flex;
    flex-wrap: wrap;
    margin: ${({ theme }) => theme.spacing(0, -1)};

    & > * {
      flex-shrink: 0;
      margin: ${({ theme }) => theme.spacing(0, 1)};
      margin-top: ${({ theme }) => theme.spacing(0.5)};
    }

    font-size: ${({ theme }) => theme.typography.body2.fontSize}px;
    justify-content: space-between;
  }

  & .legendDot {
    width: ${DOT_SIZE}px;
    height: ${DOT_SIZE}px;
    border-radius: ${DOT_SIZE / 2}px;
    filter: brightness(
      ${({ theme }) => (theme.palette.mode === 'dark' ? 0.8 : 1)}
    );
  }
`;

type RelevantState = Exclude<State, 'DISABLED'>;

const STATES_ORDER = [
  'REVIEWED',
  'TRANSLATED',
  'UNTRANSLATED',
] as RelevantState[];

export function TranslationStatesBar(props: {
  labels: boolean;
  hideTooltips?: boolean;
  stats: {
    keyCount: number;
    languageCount: number;
    translationStatePercentages: {
      [state in RelevantState]: number;
    };
  };
}) {
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    const timer = setTimeout(() => setLoaded(true), 50);
    return () => clearTimeout(timer);
  }, []);

  const LegendItem = (legendItemProps: { state: State }) => {
    const percent =
      props.stats.translationStatePercentages[legendItemProps.state];

    return (
      <Box display="flex" alignItems="center" mr={2}>
        <Box
          data-cy="project-states-bar-dot"
          mr={1}
          className="legendDot"
          style={{
            backgroundColor: TRANSLATION_STATES[legendItemProps.state].color,
          }}
        />
        {TRANSLATION_STATES[legendItemProps.state].translation}:&nbsp;
        {percent >= 1 ? (
          <T
            keyName="project_dashboard_translations_percent"
            params={{
              percent: percent / 100,
            }}
          />
        ) : (
          <T keyName="project_dashboard_translations_less_then_1_percent" />
        )}
      </Box>
    );
  };

  const visibleStates = STATES_ORDER.filter(
    (state) => props.stats.translationStatePercentages[state]
  ).filter((state) => TRANSLATION_STATES[state]);

  return (
    <StyledContainer data-cy="project-states-bar-root">
      <div className="bar" data-cy="project-states-bar-bar">
        {visibleStates.map((state, idx) => (
          <Tooltip
            key={idx}
            title={TRANSLATION_STATES[state].translation}
            open={props.hideTooltips ? false : undefined}
          >
            <Box
              data-cy="project-states-bar-state-progress"
              className={clsx({
                state: true,
                loadedState: !loaded,
              })}
              style={{
                zIndex: 5 - idx,
                width: `calc(max(${props.stats.translationStatePercentages[state]}%, 8px) + ${BORDER_RADIUS}px)`,
                backgroundColor: TRANSLATION_STATES[state].color,
              }}
            />
          </Tooltip>
        ))}
      </div>
      {props.labels && (
        <Box className="legend" data-cy="project-states-bar-legend">
          {visibleStates.map((state, i) => (
            <LegendItem key={i} state={state} />
          ))}
        </Box>
      )}
    </StyledContainer>
  );
}
