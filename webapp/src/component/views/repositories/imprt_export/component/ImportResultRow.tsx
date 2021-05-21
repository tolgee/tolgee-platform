import {components} from "../../../../../service/apiSchema";
import React from "react";
import {Box, Button, IconButton, Link, makeStyles, TableCell, TableRow, useTheme} from "@material-ui/core";
import DeleteIcon from "@material-ui/icons/Delete";
import {ImportActions} from "../../../../../store/repository/ImportActions";
import {container} from "tsyringe";
import {useRepository} from "../../../../../hooks/useRepository";
import {confirmation} from "../../../../../hooks/confirmation";
import {ImportRowLanguageMenu} from "./ImportRowLanguageMenu";
import {CheckCircle, Warning} from "@material-ui/icons";
import EditIcon from "@material-ui/icons/Edit";

const useStyles = makeStyles(theme => ({
    root: {
        "&:hover": {
            backgroundColor: theme.palette.grey["50"]
        },
        "&:hover $pencil": {
            opacity: 1
        }
    },
    resolvedIcon: {
        fontSize: 16,
        marginRight: 4,
        color: theme.palette.success.main
    },
    resolveButton: {
        marginLeft: 25,
        paddingRight: 25 + theme.spacing(0.5)
    },
    pencil: {
        fontSize: 20,
        opacity: 0,
        color: theme.palette.grey["500"],
        position: "absolute",
        right: theme.spacing(0.5)
    }
}))

const actions = container.resolve(ImportActions)
export const ImportResultRow = (props: {
    row: components["schemas"]["ImportLanguageModel"]
    onResolveConflicts: () => void,
    onShowFileIssues: () => void
    onShowData: () => void
}) => {
    const repository = useRepository()

    const classes = useStyles()

    const deleteLanguage = () => {
        confirmation({
            onConfirm: () => actions.loadableActions.deleteLanguage.dispatch({path: {languageId: props.row.id, repositoryId: repository.id}})
        })
    };

    const theme = useTheme()

    return (
        <React.Fragment>
            <TableRow className={classes.root}>
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
                <TableCell scope="row" align="center">
                    <Link href="#" onClick={() => {
                        props.onShowData()
                    }}>{props.row.totalCount}</Link>
                </TableCell>
                <TableCell scope="row" align="center">
                    <Button disabled={props.row.conflictCount < 1} onClick={() => props.onResolveConflicts()} size="small" className={classes.resolveButton}>
                        <CheckCircle className={classes.resolvedIcon}/>
                        {props.row.resolvedCount} / {props.row.conflictCount}
                        {props.row.conflictCount > 0 && <EditIcon className={classes.pencil}/>}
                    </Button>
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
