import {default as React, FunctionComponent} from 'react';
import CircularProgress from '@material-ui/core/CircularProgress';
import makeStyles from '@material-ui/core/styles/makeStyles';
import Backdrop from '@material-ui/core/Backdrop';
import createStyles from "@material-ui/core/styles/createStyles";
import {Theme} from "@material-ui/core";

interface FullPageLoadingProps {

}

const useStyles = makeStyles((theme: Theme) =>
    createStyles({
        backdrop: {
            zIndex: theme.zIndex.drawer + 1,
            color: '#fff',
        },
    }),
);

export const FullPageLoading: FunctionComponent<FullPageLoadingProps> = (props) => {
    const classes = useStyles({});

    return (
        <Backdrop className={classes.backdrop} open={true}>
            <CircularProgress color="inherit"/>
        </Backdrop>
    );
};
