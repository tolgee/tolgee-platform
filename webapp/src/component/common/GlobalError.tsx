import * as React from 'react';
import {Button, Paper} from '@material-ui/core';
import Box from '@material-ui/core/Box';
import Typography from '@material-ui/core/Typography';
import Container from '@material-ui/core/Container';
import {GlobalError as GlobalErrorError} from "../../error/GlobalError";
import {SadGoatMessage} from "./SadGoatMessage";
import {GlobalActions} from "../../store/global/globalActions";
import {container} from "tsyringe";

export default function GlobalError(props: { error: GlobalErrorError }) {
    let dev = environment.mode === "development";

    return (
        <Container maxWidth={dev ? 'lg' : 'sm'}>
            <Box mt={5}>
                <Paper>
                    <Box>
                        <Box p={4}>
                            <Box mb={5}>
                                <Typography variant="h4">Unexpected error occurred</Typography>
                            </Box>

                            {!dev &&
                            <Box>
                                <SadGoatMessage>{null}</SadGoatMessage>
                            </Box>}

                            {props.error.publicInfo &&
                            <Box mb={5}>
                                <Typography variant="h4">{props.error.publicInfo}</Typography>
                            </Box>}

                            <Box display="flex" justifyContent="center" p={3}>
                                <Button size="large" variant="outlined" color="primary"
                                        onClick={() => {
                                            container.resolve(GlobalActions).logout.dispatch()
                                        }}>Start over!</Button>
                            </Box>

                            <Typography variant='body1'>The error is logged and we will fix this soon. Now please try to reload this page.</Typography>
                            {dev &&
                            <Box mt={5}>
                                {props.error.debugInfo &&
                                <>
                                    <Typography variant="h5">Debug information</Typography>
                                    <pre>
                                            {props.error.debugInfo}
                                    </pre>
                                </>
                                }
                                <Typography variant="h5">Stack trace</Typography>
                                <pre>
                                        {props.error.stack}
                                    </pre>

                                {props.error.e &&
                                <pre>
                                        {props.error.e && props.error.e.stack}
                                    </pre>
                                }
                            </Box>
                            }
                        </Box>
                    </Box>
                </Paper>
            </Box>
        </Container>
    );
}
