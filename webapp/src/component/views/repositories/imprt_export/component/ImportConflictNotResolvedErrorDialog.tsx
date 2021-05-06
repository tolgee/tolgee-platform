import React, {FunctionComponent} from 'react';
import {Button, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle} from "@material-ui/core";
import {T} from "@tolgee/react";
import {useRepository} from "../../../../../hooks/useRepository";
import {container} from "tsyringe";
import {ImportActions} from "../../../../../store/repository/ImportActions";

const actions = container.resolve(ImportActions)
export const ImportConflictNotResolvedErrorDialog: FunctionComponent<{
    open: boolean,
    onClose: () => void
}> = (props) => {
    const repository = useRepository()

    const doForce = (forceMode) => {
        actions.loadableActions.applyImport.dispatch({
            path: {
                repositoryId: repository.id
            },
            query: {
                forceMode: forceMode
            }
        })
        props.onClose()
    }

    return (
        <div>
            <Dialog open={props.open} onClose={props.onClose} aria-labelledby="import-not-resolved-error-dialog-title">
                <DialogTitle id="import-not-resolved-error-dialog-title"><T>import_not_resolved_error_dialog_title</T></DialogTitle>
                <DialogContent>
                    <DialogContentText>
                        <T>import_not_resolved_error_dialog_message_text</T>
                    </DialogContentText>
                </DialogContent>
                <DialogActions>
                    <Button onClick={props.onClose} color="secondary">
                        <T>import_not_resolved_error_dialog_cancel_button</T>
                    </Button>
                    <Button onClick={() => doForce("KEEP")} color="primary">
                        <T>import_keep_existing_for_all_button</T>
                    </Button>
                    <Button onClick={() => doForce("OVERRIDE")} color="primary">
                        <T>import_override_all_button</T>
                    </Button>
                </DialogActions>
            </Dialog>
        </div>
    );
};
