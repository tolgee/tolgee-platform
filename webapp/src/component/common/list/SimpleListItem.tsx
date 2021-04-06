import {ComponentProps, default as React, FunctionComponent} from 'react';
import ListItem from '@material-ui/core/ListItem';
import makeStyles from "@material-ui/core/styles/makeStyles";
import {Theme} from "@material-ui/core";
import createStyles from "@material-ui/core/styles/createStyles";

const useStyles = makeStyles((theme: Theme) =>
    createStyles({
        container: {
            borderBottom: `1px solid ${theme.palette.grey.A100}`,
            "&:last-child": {
                borderBottom: `none`,
            }
        }
    }),
);

export const SimpleListItem: FunctionComponent<ComponentProps<typeof ListItem>> = (props) => {
    const classes = useStyles();

    return (
        <ListItem {...props} classes={{container: classes.container}}>
            {props.children}
        </ListItem>
    );
}
