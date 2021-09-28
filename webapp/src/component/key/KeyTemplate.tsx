import { makeStyles } from '@material-ui/core';

const useStyles = makeStyles((theme) => ({
  container: {
    display: 'inline-flex',
    alignItems: 'center',
    background:
      'linear-gradient(-225deg, rgb(213, 219, 228) 0%, rgb(248, 248, 248) 100%)',
    borderRadius: 3,
    boxShadow:
      'inset 0 -2px 0 0 rgb(205, 205, 230), inset 0 0 1px 1px #fff, 0 1px 2px 1px rgba(30, 35, 90, 0.4);',
    color: 'rgb(127, 132, 151)',
    height: '18px',
    justifyContent: 'center',
    margin: '0px 4px',
    paddingBottom: '2px',
    position: 'relative',
    top: '-1px',
    width: '20px',
    verticalAlign: 'bottom',
    fontSize: theme.typography.caption.fontSize,
  },
}));

export const KeyTemplate: React.FC = ({ children }) => {
  const classes = useStyles();
  return <span className={classes.container}>{children}</span>;
};
