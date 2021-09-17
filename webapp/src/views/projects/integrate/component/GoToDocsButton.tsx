import { Button, ButtonProps, Link, makeStyles } from '@material-ui/core';
import { T } from '@tolgee/react';

const useStyles = makeStyles((t) => ({
  root: {
    marginLeft: t.spacing(2),
    '&:hover': {
      textDecoration: 'none',
    },
  },
}));

export const GoToDocsButton = (props: ButtonProps & { href: string }) => {
  const classes = useStyles();

  return (
    <>
      <Button
        // @ts-ignore
        component={Link}
        className={classes.root}
        target="_blank"
        size="large"
        color="primary"
        variant="contained"
        style={{ ...props.style }}
        data-cy="integrate-go-to-docs-button"
        {...props}
      >
        <T>integrate_guides_go_to_docs</T>
      </Button>
    </>
  );
};
