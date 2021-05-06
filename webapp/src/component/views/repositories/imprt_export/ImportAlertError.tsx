import React, {FunctionComponent, ReactNode, useEffect, useState} from 'react';
import {useImportDataHelper} from "./hooks/useImportDataHelper";
import {Box, Button, Collapse, IconButton} from "@material-ui/core";
import {container} from "tsyringe";
import {ImportActions} from "../../../../store/repository/ImportActions";
import {Alert, AlertTitle} from "@material-ui/lab";
import {T} from "@tolgee/react";
import CloseIcon from "@material-ui/icons/Close";

const actions = container.resolve(ImportActions)
export const ImportAlertError: FunctionComponent<{
    dataHelper: ReturnType<typeof useImportDataHelper>
}> = () => {
    const [moreOpen, setMoreOpen] = useState(false)
    const [collapsed, setCollapsed] = useState(false)

    const addFilesLoadable = actions.useSelector(s => s.loadables.addFiles)

    const error = addFilesLoadable?.error
    let text = undefined as ReactNode | undefined
    let params = [] as string[]

    if (error?.code === "cannot_parse_file") {
        text = <T>import_error_cannot_parse_file</T>
        params = error.params;
    }

    useEffect(() => {
        setCollapsed(false)
    }, [addFilesLoadable.loading])

    const open = !collapsed && !!text

    return (
        <Collapse in={open}>
            <Box mt={4}>
                <Alert color="error" action={
                    <>
                        <Box display="inline" mr={1}>
                            <Button color="inherit" size="small" onClick={() => setMoreOpen(!moreOpen)}>
                                {moreOpen ?
                                    <T>import_error_less_button</T>
                                    :
                                    <T>import_error_more_button</T>}
                            </Button>
                        </Box>
                        <IconButton
                            aria-label="close"
                            color="inherit"
                            size="small"
                            onClick={() => {
                                setCollapsed(true);
                            }}
                        >
                            <CloseIcon fontSize="inherit"/>
                        </IconButton>
                    </>
                }
                >
                    <AlertTitle>{text}</AlertTitle>
                    {params[0] &&
                    <T parameters={{
                        name: params[0]
                    }}>import_cannot_parse_file_message</T>}
                    <Box pt={2}>
                        {moreOpen && params[1]}
                    </Box>
                </Alert>
            </Box>
        </Collapse>
    );
};
