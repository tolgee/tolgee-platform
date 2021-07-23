import { translationStates } from 'tg.constants/translationStates';
import makeStyles from '@material-ui/core/styles/makeStyles';
import { Box, Tooltip } from '@material-ui/core';
import { T } from '@tolgee/react';

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
  },
  legend: {
    display: 'flex',
    flexWrap: 'wrap',
    marginLeft: -theme.spacing(1),
    '& > *': {
      marginLeft: theme.spacing(1),
      marginTop: theme.spacing(0.5),
    },
    fontSize: 12,
  },
  legendDot: {
    width: DOT_SIZE,
    height: DOT_SIZE,
    borderRadius: DOT_SIZE / 2,
  },
}));

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
  const percents = Object.entries(props.stats.translationStateCounts).reduce(
    (acc, [state, count]) => ({
      ...acc,
      [state]: (count / translationsCount) * 100,
    }),
    {} as { [state in States]: number }
  );

  return (
    <Box className={classes.root}>
      <Box className={classes.bar}>
        {(
          [
            'REVIEWED',
            'TRANSLATED',
            'MACHINE_TRANSLATED',
            'NEEDS_REVIEW',
            'UNTRANSLATED',
          ] as States[]
        ).map((state, idx) => (
          <Tooltip
            key={idx}
            title={<T noWrap>{translationStates[state].translationKey}</T>}
          >
            <Box
              className={classes.state}
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
        {(
          [
            'REVIEWED',
            'TRANSLATED',
            'MACHINE_TRANSLATED',
            'NEEDS_REVIEW',
            'UNTRANSLATED',
          ] as States[]
        ).map(
          (state, idx) =>
            props.stats.translationStateCounts[state] > 0 && (
              <Box key={idx} display="flex" alignItems="center" mr={2}>
                <Box
                  mr={0.5}
                  className={classes.legendDot}
                  style={{ backgroundColor: translationStates[state].color }}
                />
                <T>{translationStates[state].translationKey}</T>:{' '}
                {props.stats.translationStateCounts[state]}
              </Box>
            )
        )}
      </Box>
    </Box>
  );
}
