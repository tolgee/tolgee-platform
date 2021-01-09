import {default as React, FunctionComponent, useEffect} from 'react';
import {useRouteMatch} from 'react-router-dom';
import {PARAMS} from '../../../../constants/links';
import {BaseView} from '../../../layout/BaseView';
import {Box, Button} from "@material-ui/core";
import {container} from "tsyringe";
import {ImportExportActions} from "../../../../store/repository/ImportExportActions";
import {useSelector} from "react-redux";
import {AppState} from "../../../../store";
import {useRepository} from "../../../../hooks/useRepository";
import {T} from "@polygloat/react";

const actions = container.resolve(ImportExportActions);

export const ExportView: FunctionComponent = () => {
    const match = useRouteMatch();
    const repository = useRepository();
    const repositoryId = match.params[PARAMS.REPOSITORY_ID];
    const state = useSelector((state: AppState) => state.importExport.loadables.export);

    useEffect(() => {
        if (state.loaded) {
            const url = URL.createObjectURL(state.data);
            const a = document.createElement("a");
            a.href = url;
            a.download = repository.name + ".zip";
            a.click();
            actions.loadableReset.export.dispatch();
        }
    }, [state.loading, state.loaded]);

    useEffect(() => () => {
        actions.loadableReset.export.dispatch();
    }, []);

    const onJsonExport = () => {
        actions.loadableActions.export.dispatch(repositoryId);
    };

    return (
        <BaseView title={<T>export_translations_title</T>} xs={12} md={10} lg={8}>
            <Box mt={2}>
                <Button component="a" variant="outlined" color="primary" onClick={onJsonExport}><T>export_to_json_button</T></Button>
            </Box>
        </BaseView>
    );
};
