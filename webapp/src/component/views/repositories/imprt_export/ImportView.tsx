import {default as React, FunctionComponent} from 'react';
import {Box, Button} from '@material-ui/core';
import {BaseView} from '../../../layout/BaseView';
import {T} from '@tolgee/react';
import ImportFileInput from "./component/ImportFileInput";
import {useImportRequest} from "./hooks/useImportRequest";
import {ImportResult} from "./component/ImportResult";
import {container} from "tsyringe";
import {ImportActions} from "../../../../store/repository/ImportActions";
import {useRepository} from "../../../../hooks/useRepository";


const actions = container.resolve(ImportActions)
export const ImportView: FunctionComponent = () => {
    const request = useImportRequest()
    const repository = useRepository()

    return (
        <BaseView title={<T>import_translations_title</T>} xs={12} md={10} lg={8}>
            <Box mt={2}>
                <ImportFileInput onNewFiles={request.onNewFiles}/>
                <ImportResult result={request.result}></ImportResult>
            </Box>
            {request.result &&
            <Box>
                <Button variant="outlined" color="primary" onClick={() => {
                    actions.loadableActions.cancelImport.dispatch({
                        path: {
                            repositoryId: repository.id
                        }
                    })
                }}>
                    <T>import_cancel_button</T>
                </Button>
            </Box>}
        </BaseView>
    );
};
