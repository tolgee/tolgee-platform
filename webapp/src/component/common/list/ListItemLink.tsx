import {default as React, FunctionComponent} from 'react';
import ListItem from '@material-ui/core/ListItem';
import {Link} from 'react-router-dom';

interface ListItemLinkProps {
    to: string
    selected?: boolean
}

export const ListItemLink: FunctionComponent<ListItemLinkProps> = (props) => (
    <ListItem button component={Link} to={props.to} selected={props.selected}>
        {props.children}
    </ListItem>);
