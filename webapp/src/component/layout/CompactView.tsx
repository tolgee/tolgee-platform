import { makeStyles, Typography, Paper, IconButton } from '@material-ui/core';
import { ArrowBack } from '@material-ui/icons';
import { Link } from 'react-router-dom';

const useStyles = makeStyles((theme) => ({
  '@global': {
    body: {
      backgroundColor: theme.palette.extraLightBackground.main,
    },
  },
  container: {
    flexDirection: 'column',
    flexGrow: 1,
    alignItems: 'center',
    margin: `${theme.spacing(1)}px auto`,
    maxWidth: 430,
  },
  alerts: {
    display: 'flex',
    flexDirection: 'column',
    minHeight: 100,
    justifyContent: 'flex-end',
  },
  paper: {
    display: 'flex',
    alignItems: 'stretch',
    padding: theme.spacing(4, 0),
    marginTop: theme.spacing(2),
  },
  verticalSpace: {
    display: 'flex',
    width: theme.spacing(7),
    alignItems: 'flex-start',
    justifyContent: 'center',
    marginTop: -7,
    flexShrink: 0,
  },
  content: {
    flexGrow: 1,
  },
  footer: {
    padding: theme.spacing(1, 7),
  },
}));

type Props = {
  backLink?: string;
  alerts?: React.ReactNode;
  title: React.ReactNode;
  content: React.ReactNode;
  footer?: React.ReactNode;
};

export const CompactView: React.FC<Props> = ({
  content,
  title,
  footer,
  alerts,
  backLink,
}) => {
  const classes = useStyles();

  return (
    <div className={classes.container}>
      <div className={classes.alerts}>{alerts}</div>
      <Paper className={classes.paper}>
        <div className={classes.verticalSpace}>
          {backLink && (
            <IconButton to={backLink} component={Link}>
              <ArrowBack />
            </IconButton>
          )}
        </div>
        <div className={classes.content}>
          <Typography color="textSecondary" variant="h5">
            {title}
          </Typography>
          <div>{content}</div>
        </div>
        <div className={classes.verticalSpace}></div>
      </Paper>
      <div className={classes.footer}>{footer}</div>
    </div>
  );
};
