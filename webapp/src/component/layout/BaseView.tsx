import {default as React, ReactNode} from 'react';
import Grid from '@material-ui/core/Grid';
import {Box, Container, LinearProgress} from '@material-ui/core';
import Typography from '@material-ui/core/Typography';
import {BoxLoading} from '../common/BoxLoading';
import grey from '@material-ui/core/colors/grey';

export interface BaseViewProps {
    loading?: boolean;
    title: ReactNode;
    children: (() => ReactNode) | ReactNode;
    xs?: boolean | 'auto' | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12,
    sm?: boolean | 'auto' | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12,
    md?: boolean | 'auto' | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12,
    lg?: boolean | 'auto' | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12,
    headerChildren?: ReactNode;
    hideChildrenOnLoading?: boolean;
}

export const BaseView = (props: BaseViewProps) => {
    const hideChildrenOnLoading = props.hideChildrenOnLoading === undefined || props.hideChildrenOnLoading === true

    return (
        <Container maxWidth={false}
                   style={{
                       backgroundColor: "rgb(253,253,253)",
                       borderBottom: `1px solid ${grey[100]}`,
                       padding: 0,
                       margin: "0 -12px 0 -12px",
                       width: "calc(100% + 24px)"
                   }}>
            <Box minHeight="calc(100vh - 76px)">
                <Box style={{backgroundColor: grey[50], borderBottom: `1px solid ${grey[200]}`}} p={4} pb={2}>
                    <Grid container justify="center" alignItems="center">
                        <Grid item xs={props.xs || 12} md={props.md || 12} lg={props.lg || 12} sm={props.sm || 12}>
                            <Typography variant="h5">{props.title}</Typography>
                            {props.headerChildren && <Box mt={3}>{props.headerChildren}</Box>}
                        </Grid>
                    </Grid>
                </Box>
                <Box position="relative" overflow="visible">
                    <Box position="absolute" width="100%">
                        {props.loading && <LinearProgress/>}
                    </Box>
                </Box>
                <Box p={4} pt={2} pb={2}>
                    <Grid container justify="center" alignItems="center">
                        <Grid item xs={props.xs || 12} md={props.md || 12} lg={props.lg || 12} sm={props.sm || 12}>
                            {!props.loading || !hideChildrenOnLoading ?
                                <Box>
                                    {typeof props.children === 'function' ? props.children() : props.children}
                                </Box>
                                :
                                <BoxLoading/>
                            }
                        </Grid>
                    </Grid>
                </Box>
            </Box>
        </Container>
    );
};
