import {useEffect, useState} from "react";
import {startLoading, stopLoading} from "../../../../../hooks/loading";
import {container} from "tsyringe";
import {ImportActions} from "../../../../../store/repository/ImportActions";
import {useImportDataHelper} from "./useImportDataHelper";
import {useRepository} from "../../../../../hooks/useRepository";

const actions = container.resolve(ImportActions)
export const useApplyImportHelper = (dataHelper: ReturnType<typeof useImportDataHelper>) => {
    const [conflictNotResolvedDialogOpen, setConflictNotResolvedDialogOpen] = useState(false)

    const importApplyLoadable = actions.useSelector(s => s.loadables.applyImport)
    const repository = useRepository()
    const error = importApplyLoadable.error

    useEffect(() => {
        if (importApplyLoadable.loading) {
            startLoading()
            return
        }
        stopLoading()
    }, [importApplyLoadable.loading])

    const onApplyImport = () => {
        const unResolvedCount = dataHelper.result?._embedded?.languages?.reduce((acc, curr) => acc + curr.conflictCount - curr.resolvedCount, 0)
        if (unResolvedCount === 0) {
            actions.loadableActions.applyImport.dispatch({
                path: {
                    repositoryId: repository.id
                },
                query: {}
            })
            return
        }
        dataHelper.loadData()
        setConflictNotResolvedDialogOpen(true)
    }

    useEffect(() => {
        const error = importApplyLoadable.error
        if (error?.code == "conflict_is_not_resolved") {
            setConflictNotResolvedDialogOpen(true)
            return
        }

    }, [importApplyLoadable.error])

    useEffect(() => {
        if (importApplyLoadable.loaded) {
            dataHelper.loadData()
        }
    }, [importApplyLoadable.loaded, importApplyLoadable.loading])

    const onDialogClose = () => {
        setConflictNotResolvedDialogOpen(false)
    };

    return {onDialogClose, onApplyImport, conflictNotResolvedDialogOpen, error}
}
