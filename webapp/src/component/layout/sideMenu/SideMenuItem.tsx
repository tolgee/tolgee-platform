import ListItemIcon from '@material-ui/core/ListItemIcon';
import ListItemText from '@material-ui/core/ListItemText';
import * as React from 'react';
import {ListItemLink} from '../../common/list/ListItemLink';
import {useRouteMatch} from 'react-router-dom';

interface SideMenuItemProps {
    linkTo?: string;
    icon: React.ReactElement;
    text: string;
    selected?: boolean
}

export function SideMenuItem({linkTo, icon, text, selected}: SideMenuItemProps) {
    let match = useRouteMatch();

    return (
        <ListItemLink selected={selected ? true : match.url === linkTo} to={linkTo}>
            <ListItemIcon>
                {icon}
            </ListItemIcon>
            <ListItemText primary={text}/>
        </ListItemLink>
    );
}
