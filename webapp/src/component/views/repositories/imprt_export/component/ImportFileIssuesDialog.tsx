import React, {FunctionComponent} from 'react';
import {Box, Dialog, DialogContent, DialogTitle} from "@material-ui/core";
import {T} from "@tolgee/react";
import {useRepository} from "../../../../../hooks/useRepository";
import {container} from "tsyringe";
import {ImportActions} from "../../../../../store/repository/ImportActions";
import {components} from "../../../../../service/apiSchema";
import {SimplePaginatedHateoasList} from "../../../../common/list/SimplePaginatedHateoasList";
import {Alert} from "@material-ui/lab";
import {Warning} from "@material-ui/icons";

const actions = container.resolve(ImportActions)
export const ImportFileIssuesDialog: FunctionComponent<{
        row?: components["schemas"]["ImportLanguageModel"]
        onClose: () => void
    }> = (props) => {
        const repository = useRepository()

        const row = props.row

        return (
            <div>
                <Dialog maxWidth="lg" open={!!row} onClose={props.onClose} aria-labelledby="import-file-issues-dialog">
                    {row &&
                    <>
                        <DialogTitle id="import-file-issues-dialog">
                            <T parameters={{fileName: row.importFileName}}>import_file_issues_title</T>
                        </DialogTitle>
                        <DialogContent>
                            <SimplePaginatedHateoasList
                                actions={actions}
                                loadableName="getFileIssues"
                                dispatchParams={[{
                                    path: {
                                        repositoryId: repository.id,
                                        importFileId: row.importFileId
                                    }
                                }]}
                                wrapperComponent={Box}
                                listComponent={Box}
                                sortBy={[]}
                                renderItem={i => <>
                                    <Alert color="warning" icon={<Warning/>}>
                                        {i.type && <T>{`file_issue_type_${i.type!!.toLowerCase()}`}</T>}
                                        &nbsp;({i.params && i.params!!.map(
                                        (param, idx) =>
                                            param.value &&
                                            <>
                                                <T parameters={{value: param.value!!}}>{`import_file_issue_param_type_${param.type.toLowerCase()}`}</T>
                                                {(idx < i.params!!.length - 1) && ", "}
                                            </>
                                    )}
                                        )
                                    </Alert>
                                </>}
                            />
                        </DialogContent>
                    </>
                    }
                </Dialog>
            </div>
        );
    }
;
