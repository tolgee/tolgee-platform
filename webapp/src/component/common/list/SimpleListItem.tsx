import { ComponentProps, default as React, FunctionComponent } from 'react';
import { Theme } from '@mui/material';
import ListItem from '@mui/material/ListItem';
import createStyles from '@mui/styles/createStyles';
import makeStyles from '@mui/styles/makeStyles';

const useStyles = makeStyles<Theme>((theme) =>
  createStyles({
    container: {
      borderBottom: `1px solid ${theme.palette.grey.A100}`,
      '&:last-child': {
        borderBottom: `none`,
      },
    },
  })
);

type PropTypes = Omit<ComponentProps<typeof ListItem>, 'button'> & {
  button?: boolean;
};

export const SimpleListItem: FunctionComponent<PropTypes> = (props) => {
  const classes = useStyles();

  return (
    <ListItem
      data-cy="global-list-item"
      {...props}
      button={props.button as any}
      classes={{ container: classes.container }}
    >
      {props.children}
    </ListItem>
  );
};
