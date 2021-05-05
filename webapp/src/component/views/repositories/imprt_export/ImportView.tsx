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

const actions = container.resolve(ImportActions)
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
        if ((deleteLanguageLoadable.loaded && !deleteLanguageLoadable.loading) || selectLanguageLoadable.loaded) {
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

    return (
        <BaseView title={<T>import_translations_title</T>} xs={12} md={10} lg={8}>
            <Box mt={2}>
                <ImportFileInput onNewFiles={dataHelper.onNewFiles}/>
                <ImportResult onLoadData={dataHelper.loadData} result={dataHelper.result}/>
            </Box>
            {dataHelper.result &&
            <Box display="flex" mt={2} justifyContent="flex-end">
                <Box mr={2}>
                    <Button variant="outlined" color="primary" onClick={() => {
                        actions.loadableActions.cancelImport.dispatch({
                            path: {
                                repositoryId: repository.id
                            }
                        })
                    }}>
                        <T>import_cancel_button</T>
                    </Button>
                </Box>
                <Box>
                    <Button variant="contained" color="primary" onClick={applyImportHelper.onApplyImport}>
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
