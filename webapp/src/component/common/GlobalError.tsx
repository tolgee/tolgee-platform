import { Button, makeStyles, Theme } from '@material-ui/core';
import Box from '@material-ui/core/Box';
import Typography from '@material-ui/core/Typography';
import { container } from 'tsyringe';

import { GlobalError as GlobalErrorError } from 'tg.error/GlobalError';
import { GlobalActions } from 'tg.store/global/GlobalActions';

const useStyles = makeStyles((theme: Theme) => ({
  image: {
    filter: 'grayscale(50%)',
    opacity: '0.7',
    maxWidth: '100%',
    width: 500,
  },
}));
export default function GlobalError(props: { error: GlobalErrorError }) {
  const classes = useStyles();
  const dev = process.env.NODE_ENV === 'development';

  return (
    <Box p={4}>
      <Box mb={5}>
        <Typography variant="h4">Unexpected error occurred</Typography>
      </Box>

      {!dev && (
        <Box
          display="flex"
          justifyContent="center"
          flexDirection="column"
          alignItems="center"
        >
          <img
            src="/images/brokenMouse.svg"
            draggable="false"
            className={classes.image}
          />
        </Box>
      )}

      {props.error.publicInfo && (
        <Box mb={5}>
          <Typography variant="h4">{props.error.publicInfo}</Typography>
        </Box>
      )}

      <Box display="flex" justifyContent="center" p={3}>
        <Button
          size="large"
          variant="outlined"
          color="primary"
          onClick={() => {
            container.resolve(GlobalActions).logout.dispatch();
            location.reload();
          }}
        >
          Start over!
        </Button>
      </Box>

      <Typography variant="body1">
        The error is logged and we will fix this soon. Now please try to reload
        this page.
      </Typography>
      {dev && (
        <Box mt={5}>
          {props.error.debugInfo && (
            <>
              <Typography variant="h5">Debug information</Typography>
              <pre>{props.error.debugInfo}</pre>
            </>
          )}
          <Typography variant="h5">Stack trace</Typography>
          <pre>{props.error.stack}</pre>

          {props.error.e && <pre>{props.error.e && props.error.e.stack}</pre>}
        </Box>
      )}
    </Box>
  );
}
