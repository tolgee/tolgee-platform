import {default as React, FunctionComponent, ReactNode} from 'react';
import makeStyles from '@material-ui/core/styles/makeStyles';
import createStyles from "@material-ui/core/styles/createStyles";
import {Theme, Typography} from "@material-ui/core";
import Box from "@material-ui/core/Box";
import SadGoat from '../../svgs/sadGoat.svg'

const useStyles = makeStyles((theme: Theme) =>
    createStyles({
        goat: {
            filter: "grayscale(75%) blur(0.3px)",
            opacity: "0.09",
        },
        text: {
            opacity: "0.3"
        }
    }),
);

export const SadGoatMessage: FunctionComponent<{ children: ReactNode }> = (props) => {
    const classes = useStyles({});

    return (
        <>
            <Box display="flex" justifyContent="center" flexDirection="column" alignItems="center">
                <Box className={classes.goat}>
                    <SadGoat width={200}/>
                </Box>
                {props.children &&
                <Box p={4} className={classes.text}>
                    <Typography>{props.children}</Typography>
                </Box>}
            </Box>
        </>
    );
};