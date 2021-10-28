import { Theme, Typography } from '@material-ui/core';
import Box from '@material-ui/core/Box';
import createStyles from '@material-ui/core/styles/createStyles';
import makeStyles from '@material-ui/core/styles/makeStyles';

const useStyles = makeStyles((theme: Theme) =>
  createStyles({
    image: {
      filter: 'grayscale(50%) blur(0.3px)',
      opacity: '0.3',
      maxWidth: '100%',
    },
    text: {
      opacity: '0.3',
      paddingTop: theme.spacing(4),
    },
  })
);

type Props = {
  hint?: React.ReactNode;
};

export const SadEmotionMessage: React.FC<Props> = (props) => {
  const classes = useStyles({});

  return (
    <Box
      display="flex"
      justifyContent="center"
      flexDirection="column"
      alignItems="center"
    >
      {props.children && (
        <Box className={classes.text}>
          <Typography>{props.children}</Typography>
        </Box>
      )}
      <img
        src="/images/sleepingMouse.svg"
        draggable="false"
        className={classes.image}
        width="397px"
        height="300px"
      />
      {props.hint && <Box py={4}>{props.hint}</Box>}
    </Box>
  );
};
