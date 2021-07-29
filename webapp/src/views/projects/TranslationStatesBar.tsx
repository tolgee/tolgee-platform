import { translationStates } from 'tg.constants/translationStates';
import makeStyles from '@material-ui/core/styles/makeStyles';
import { Box, Tooltip } from '@material-ui/core';
import { T } from '@tolgee/react';
import clsx from 'clsx';
import { useEffect, useState } from 'react';

type States = keyof typeof translationStates;

const HEIGHT = 6;
const BORDER_RADIUS = HEIGHT / 2;
const DOT_SIZE = 6;
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
  },
  state: {
    height: '100%',
    overflow: 'hidden',
    borderRadius: BORDER_RADIUS,
    marginLeft: -BORDER_RADIUS,
    transition: 'width 1s ease-in-out',
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
    fontSize: 14,
  },
  legendDot: {
    width: DOT_SIZE,
    height: DOT_SIZE,
    borderRadius: DOT_SIZE / 2,
  },
}));

const STATES_ORDER = [
  'NEEDS_REVIEW',
  'REVIEWED',
  'TRANSLATED',
  'MACHINE_TRANSLATED',
  'UNTRANSLATED',
] as States[];

export function TranslationStatesBar(props: {
  stats: {
    projectId: number;
    keyCount: number;
    languageCount: number;
    translationStateCounts: {
      [state in States]: number;
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
    {} as { [state in States]: number }
  );

  useEffect(() => {
    setTimeout(() => setLoaded(true), 50);
  }, []);

  return (
    <Box className={classes.root}>
      <Box className={classes.bar}>
        {STATES_ORDER.map((state, idx) => (
          <Tooltip
            key={idx}
            title={<T noWrap>{translationStates[state].translationKey}</T>}
          >
            <Box
              className={clsx({
                [classes.state]: true,
                [classes.loadedState]: !loaded,
              })}
              style={{
                visibility: percents[state] < 0.01 ? 'hidden' : 'initial',
                zIndex: 5 - idx,
                width: `calc(${percents[state]}% + ${BORDER_RADIUS}px)`,
                backgroundColor: translationStates[state].color,
              }}
            />
          </Tooltip>
        ))}
      </Box>
      <Box className={classes.legend}>
        {STATES_ORDER.map(
          (state, idx) =>
            props.stats.translationStateCounts[state] > 0 && (
              <Box key={idx} display="flex" alignItems="center" mr={2}>
                <Box
                  mr={0.5}
                  className={classes.legendDot}
                  style={{ backgroundColor: translationStates[state].color }}
                />
                <T>{translationStates[state].translationKey}</T>:{' '}
                <T
                  parameters={{
                    keyCount:
                      props.stats.translationStateCounts[state].toString(),
                  }}
                >
                  translation_states_count
                </T>
              </Box>
            )
        )}
      </Box>
    </Box>
  );
}
