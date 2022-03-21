import { Theme, Typography } from '@mui/material';
import Box from '@mui/material/Box';
import createStyles from '@mui/styles/createStyles';
import makeStyles from '@mui/styles/makeStyles';

const useStyles = makeStyles<Theme>((theme) =>
  createStyles({
    image: {
      filter: 'grayscale(50%)',
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
