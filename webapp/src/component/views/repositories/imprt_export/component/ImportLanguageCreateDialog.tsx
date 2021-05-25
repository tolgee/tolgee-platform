import React, {FunctionComponent} from 'react';
import {Box, Dialog, DialogContent, DialogTitle} from "@material-ui/core";
import {T} from "@tolgee/react";
import {LanguageCreateForm} from "../../../../languages/LanguageCreateForm";


export const ImportLanguageCreateDialog: FunctionComponent<{
    open: boolean
    onCreated: (id: number) => void
    onClose: () => void
}> = (props) => {

    return (
        <Dialog open={props.open} aria-labelledby="form-dialog-title">
            <DialogTitle id="form-dialog-title"><T>import_add_new_language_dialog_title</T></DialogTitle>
            <DialogContent>
                <Box mt={-2}>
                    <LanguageCreateForm
                        onCreated={(language) => {props.onCreated(language.id)}}
                        onCancel={props.onClose}
                    />
                </Box>
            </DialogContent>
        </Dialog>
    );
};
