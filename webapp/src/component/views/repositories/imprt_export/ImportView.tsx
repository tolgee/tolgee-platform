import {default as React, FunctionComponent} from 'react';
import {Box} from '@material-ui/core';
import {BaseView} from '../../../layout/BaseView';
import {T} from '@tolgee/react';
import ImportFileInput from "./component/ImportFileInput";
import {useImportRequest} from "./useImportRequest";
import {ImportResult} from "./component/ImportResult";

export const ImportView: FunctionComponent = () => {
    const request = useImportRequest()

    return (
        <BaseView title={<T>import_translations_title</T>} xs={12} md={10} lg={8}>
            <Box mt={2}>
                <ImportFileInput onNewFiles={request.onNewFiles}/>
                <ImportResult result={request.result}></ImportResult>
            </Box>
        </BaseView>
    );
};
