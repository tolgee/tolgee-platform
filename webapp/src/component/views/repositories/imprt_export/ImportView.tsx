import {default as React, FunctionComponent, useEffect} from 'react';
import {Box, Button} from '@material-ui/core';
import {BaseView} from '../../../layout/BaseView';
import {T} from '@tolgee/react';
import ImportFileInput from "./component/ImportFileInput";
import {useImportDataHelper} from "./hooks/useImportDataHelper";
import {ImportResult} from "./component/ImportResult";
import {container} from "tsyringe";
import {ImportActions} from "../../../../store/repository/ImportActions";
import {useRepository} from "../../../../hooks/useRepository";
import {ImportConflictNotResolvedErrorDialog} from "./component/ImportConflictNotResolvedErrorDialog";
import {useApplyImportHelper} from "./hooks/useApplyImportHelper";
import {stopLoading} from "../../../../hooks/loading";
import {parseErrorResponse} from "../../../../fixtures/errorFIxtures";
import {MessageService} from "../../../../service/MessageService";
import {ImportAlertError} from './ImportAlertError';
import {confirmation} from "../../../../hooks/confirmation";

const actions = container.resolve(ImportActions)
const messageService = container.resolve(MessageService)

export const ImportView: FunctionComponent = () => {
    const dataHelper = useImportDataHelper()
    const repository = useRepository()
    const applyImportHelper = useApplyImportHelper(dataHelper)
    const cancelLoadable = actions.useSelector(s => s.loadables.cancelImport)
    const deleteLanguageLoadable = actions.useSelector(s => s.loadables.deleteLanguage)
    const addFilesLoadable = actions.useSelector(s => s.loadables.addFiles)
    const resultLoadable = actions.useSelector(s => s.loadables.getResult)
    const resultLoading = resultLoadable.loading || addFilesLoadable.loading
    const selectLanguageLoadable = actions.useSelector(s => s.loadables.selectLanguage)

    useEffect(() => {
        if (!resultLoading) {
            stopLoading()
        }

        const error = resultLoadable.error
        if (error?.code === "resource_not_found") {
            dataHelper.resetResult()
        }

    }, [resultLoadable.loading, addFilesLoadable.loading])

    useEffect(() => {
        if ((deleteLanguageLoadable.loaded && !deleteLanguageLoadable.loading) ||
            (selectLanguageLoadable.loaded && !selectLanguageLoadable.loading)) {
            dataHelper.loadData()
        }
    }, [deleteLanguageLoadable.loading, selectLanguageLoadable.loading])

    useEffect(() => {
        if (!resultLoading) {
            dataHelper.loadData()
        }
    }, [])

    useEffect(() => {
        if (cancelLoadable.loaded) {
            dataHelper.resetResult()
        }
    }, [cancelLoadable.loading])

    useEffect(() => {
        if (applyImportHelper.error) {
            const parsed = parseErrorResponse(applyImportHelper.error)
            messageService.error(<T>{parsed[0]}</T>)
            actions.loadableReset.applyImport.dispatch()
        }
    }, [applyImportHelper.error])

    const onApply = () => {
        actions.touchApply.dispatch()
        if (dataHelper.isValid) {
            applyImportHelper.onApplyImport()
        }
    }

    return (
        <BaseView title={<T>import_translations_title</T>} xs={12} md={10} lg={8}>
            <Box mt={2}>
                <ImportFileInput onNewFiles={dataHelper.onNewFiles}/>
                {addFilesLoadable.data?.errors?.map((e, idx) =>
                    <ImportAlertError key={idx} error={e}/>
                )}
                <ImportResult onLoadData={dataHelper.loadData} result={dataHelper.result}/>
            </Box>
            {dataHelper.result &&
            <Box display="flex" mt={2} justifyContent="flex-end">
                <Box mr={2}>
                    <Button variant="outlined" color="primary" onClick={() => {
                        confirmation({
                                onConfirm: () => actions.loadableActions.cancelImport.dispatch({
                                    path: {
                                        repositoryId: repository.id
                                    }
                                }),
                                title: <T>import_cancel_confirmation_title</T>,
                                message: <T>import_cancel_confirmation_message</T>
                            }
                        )

                    }}>
                        <T>import_cancel_button</T>
                    </Button>
                </Box>
                <Box>
                    <Button variant="contained" color="primary" onClick={onApply}>
                        <T>import_apply_button</T>
                    </Button>
                </Box>
            </Box>}
            <ImportConflictNotResolvedErrorDialog
                open={applyImportHelper.conflictNotResolvedDialogOpen}
                onClose={applyImportHelper.onDialogClose}
            />
        </BaseView>
    );
};
