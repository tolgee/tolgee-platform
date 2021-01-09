import * as React from 'react';
import clsx from 'clsx';
import {makeStyles} from '@material-ui/core/styles';
import AppBar from '@material-ui/core/AppBar';
import Toolbar from '@material-ui/core/Toolbar';
import Typography from '@material-ui/core/Typography';
import {Link} from "react-router-dom";
import {PolygloatLogo} from "../common/icons/PolygloatLogo";
import {Box} from "@material-ui/core";
import {UserMenu} from "../security/UserMenu";
import {LocaleMenu} from "../LocaleMenu";

const drawerWidth = 240;

const useStyles = makeStyles(theme => ({
    appBar: {
        zIndex: theme.zIndex.drawer + 1,
        transition: theme.transitions.create(['width', 'margin'], {
            easing: theme.transitions.easing.sharp,
            duration: theme.transitions.duration.leavingScreen,
        }),
    },
    appBarShift: {
        marginLeft: drawerWidth,
        width: `calc(100% - ${drawerWidth}px)`,
        transition: theme.transitions.create(['width', 'margin'], {
            easing: theme.transitions.easing.sharp,
            duration: theme.transitions.duration.enteringScreen,
        }),
    },
    toolbar: {
        paddingRight: 24, // keep right padding when drawer closed
    },
    menuButton: {
        marginRight: 36,
    },
    menuButtonHidden: {
        display: 'none',
    },
    polygloatLink: {
        color: "inherit",
        textDecoration: "inherit"
    }
}));

interface TopBarProps {
}

export function TopBar(props: TopBarProps) {
    const classes = useStyles({});

    return (
        <AppBar position="absolute" className={clsx(classes.appBar)}>
            <Toolbar className={classes.toolbar}>
                <Box flexGrow={1} display="flex">
                    <Box>
                        <Link className={classes.polygloatLink} to={"/"}>
                            <Box display="flex" alignItems="center">
                                <Box pr={1} display="flex" justifyItems="center">
                                    <PolygloatLogo fontSize="large"/>
                                </Box>
                                <Typography variant="h6" color="inherit">
                                    Polygloat
                                </Typography>
                            </Box>
                        </Link>
                    </Box>
                </Box>
                <Box display="inline" marginRight={1}>
                    <LocaleMenu/>
                </Box>
                <UserMenu variant="expanded"/>
            </Toolbar>
        </AppBar>
    );
}
