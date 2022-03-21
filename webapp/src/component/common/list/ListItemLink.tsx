import { default as React, FunctionComponent } from 'react';
import ListItem from '@mui/material/ListItem';
import { Link } from 'react-router-dom';

interface ListItemLinkProps {
  to: string;
  selected?: boolean;
}

export const ListItemLink: FunctionComponent<ListItemLinkProps> = (props) => (
  <ListItem
    data-cy="global-list-item"
    button
    component={Link}
    to={props.to}
    selected={props.selected}
  >
    {props.children}
  </ListItem>
);
