import {components} from "../../../../../service/apiSchema";
import React from "react";
import {Box, Button, IconButton, TableCell, TableRow, useTheme} from "@material-ui/core";
import DeleteIcon from "@material-ui/icons/Delete";
import {ImportActions} from "../../../../../store/repository/ImportActions";
import {container} from "tsyringe";
import {useRepository} from "../../../../../hooks/useRepository";
import {confirmation} from "../../../../../hooks/confirmation";
import {ImportRowLanguageMenu} from "./ImportRowLanguageMenu";
import {Warning} from "@material-ui/icons";

const actions = container.resolve(ImportActions)
export const ImportResultRow = (props: {
    row: components["schemas"]["ImportLanguageModel"]
    onResolveConflicts: () => void,
    onShowFileIssues: () => void
    onShowData: () => void
}) => {
    const repository = useRepository()

    const deleteLanguage = () => {
        confirmation({
            onConfirm: () => actions.loadableActions.deleteLanguage.dispatch({path: {languageId: props.row.id, repositoryId: repository.id}})
        })
    };

    const theme = useTheme()

    return (
        <React.Fragment>
            <TableRow>
                <TableCell scope="row">
                    <ImportRowLanguageMenu value={props.row.existingLanguageId} importLanguageId={props.row.id}/>
                </TableCell>
                <TableCell scope="row">
                    {props.row.importFileName}
                    {props.row.importFileIssueCount ?
                        <Box pt={1} ml={-1}>
                            <Button
                                onClick={() => {
                                    props.onShowFileIssues()
                                }}
                                style={{
                                    color: theme.palette.warning.main,
                                }} startIcon={<Warning/>} size="small">
                                {props.row.importFileIssueCount}
                            </Button>
                        </Box> : <></>}
                </TableCell>
                <TableCell scope="row">
                    <Button onClick={() => {
                        props.onShowData()
                    }} size="small">{props.row.totalCount}</Button>
                </TableCell>
                <TableCell scope="row">
                    <Button onClick={() => props.onResolveConflicts()} size="small">{props.row.resolvedCount}/{props.row.conflictCount}</Button>
                </TableCell>
                <TableCell scope="row" align={"right"}>
                    <IconButton onClick={deleteLanguage} size="small" style={{padding: 0}}>
                        <DeleteIcon/>
                    </IconButton>
                </TableCell>
            </TableRow>
        </React.Fragment>
    );
}
