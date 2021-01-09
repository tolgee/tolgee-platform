import Box from '@material-ui/core/Box';
import CircularProgress from '@material-ui/core/CircularProgress';
import {default as React} from 'react';

export function BoxLoading() {
    return <Box display="flex" alignItems="center" justifyContent="center" p={4}><CircularProgress/></Box>;
}
