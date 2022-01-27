import { useEffect, useState } from 'react';
import clsx from 'clsx';
import { Box, makeStyles, Tooltip } from '@material-ui/core';
import { T } from '@tolgee/react';
import { ClassNameMap } from 'notistack';

import { translationStates } from 'tg.constants/translationStates';

type State = keyof typeof translationStates;

const HEIGHT = 8;
const BORDER_RADIUS = HEIGHT / 2;
const DOT_SIZE = 8;
const useStyles = makeStyles((theme) => ({
  root: {
    width: '100%',
    minWidth: 150,
  },
  bar: {
    height: HEIGHT,
    backgroundColor: translationStates.UNTRANSLATED.color,
    borderRadius: BORDER_RADIUS,
    width: '100%',
    minWidth: 150,
    overflow: 'hidden',
    display: 'flex',
    marginBottom: theme.spacing(0.5),
  },
  state: {
    height: '100%',
    overflow: 'hidden',
    borderRadius: BORDER_RADIUS,
    marginLeft: -BORDER_RADIUS,
    transition: 'width 0.4s ease-in-out',
  },
  loadedState: {
    width: '0 !important',
  },
  legend: {
    display: 'flex',
    flexWrap: 'wrap',
    margin: `0 -${theme.spacing(1)}px`,
    '& > *': {
      flexShrink: 0,
      margin: `0 ${theme.spacing(1)}px`,
      marginTop: theme.spacing(0.5),
    },
    fontSize: theme.typography.body2.fontSize,
    justifyContent: 'space-between',
  },
  legendDot: {
    width: DOT_SIZE,
    height: DOT_SIZE,
    borderRadius: DOT_SIZE / 2,
  },
}));

const STATES_ORDER = ['REVIEWED', 'TRANSLATED', 'UNTRANSLATED'] as State[];

export function TranslationStatesBar(props: {
  labels: boolean;
  stats: {
    projectId: number;
    keyCount: number;
    languageCount: number;
    translationStateCounts: {
      [state in State]: number;
    };
  };
}) {
  const classes = useStyles();
  const translationsCount = props.stats.languageCount * props.stats.keyCount;
  const [loaded, setLoaded] = useState(false);
  const percents = Object.entries(props.stats.translationStateCounts).reduce(
    (acc, [state, count]) => ({
      ...acc,
      [state]: (count / translationsCount) * 100,
    }),
    {} as { [state in State]: number }
  );

  useEffect(() => {
    setTimeout(() => setLoaded(true), 50);
  }, []);

  const LegendItem = (legendItemProps: {
    classes: ClassNameMap<
      'legendDot' | 'bar' | 'loadedState' | 'legend' | 'root' | 'state'
    >;
    state: State;
  }) => {
    const percent =
      props.stats.translationStateCounts[legendItemProps.state] /
      (props.stats.keyCount * props.stats.languageCount);

    return (
      <Box display="flex" alignItems="center" mr={2}>
        <Box
          data-cy="project-states-bar-dot"
          mr={1}
          className={legendItemProps.classes.legendDot}
          style={{
            backgroundColor: translationStates[legendItemProps.state].color,
          }}
        />
        <T>{translationStates[legendItemProps.state].translationKey}</T>:&nbsp;
        {percent >= 0.01 ? (
          <T
            parameters={{
              percent: percent.toString(),
            }}
          >
            project_dashboard_translations_percent
          </T>
        ) : (
          <T>project_dashboard_translations_less_then_1_percent</T>
        )}
      </Box>
    );
  };

  const visibleStates = STATES_ORDER.filter((state) => percents[state]).filter(
    (state) => translationStates[state]
  );

  return (
    <Box className={classes.root} data-cy="project-states-bar-root">
      <Box className={classes.bar} data-cy="project-states-bar-bar">
        {visibleStates.map((state, idx) => (
          <Tooltip
            key={idx}
            title={<T>{translationStates[state].translationKey}</T>}
          >
            <Box
              data-cy="project-states-bar-state-progress"
              className={clsx({
                [classes.state]: true,
                [classes.loadedState]: !loaded,
              })}
              style={{
                zIndex: 5 - idx,
                width: `calc(max(${percents[state]}%, 8px) + ${BORDER_RADIUS}px)`,
                backgroundColor: translationStates[state].color,
              }}
            />
          </Tooltip>
        ))}
      </Box>
      {props.labels && (
        <Box className={classes.legend} data-cy="project-states-bar-legend">
          {visibleStates.map((state, i) => (
            <LegendItem key={i} classes={classes} state={state} />
          ))}
        </Box>
      )}
    </Box>
  );
}
