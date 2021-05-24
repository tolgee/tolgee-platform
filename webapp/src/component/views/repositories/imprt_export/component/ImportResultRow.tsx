import {components} from "../../../../../service/apiSchema";
import React from "react";
import {Box, Button, IconButton, Link, makeStyles, TableCell, TableRow, useTheme} from "@material-ui/core";
import DeleteIcon from "@material-ui/icons/Delete";
import {ImportActions} from "../../../../../store/repository/ImportActions";
import {container} from "tsyringe";
import {useRepository} from "../../../../../hooks/useRepository";
import {confirmation} from "../../../../../hooks/confirmation";
import {ImportRowLanguageMenu} from "./ImportRowLanguageMenu";
import {CheckCircle, Visibility, Warning} from "@material-ui/icons";
import EditIcon from "@material-ui/icons/Edit";
import {T} from "@tolgee/react";
import clsx from "clsx";

const useStyles = makeStyles(theme => ({
    root: {
        "&:hover": {
            backgroundColor: theme.palette.grey["50"]
        },
        "&:hover $helperIcon": {
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
        position: "absolute",
        right: theme.spacing(0.5),
    },
    helperIcon: {
        fontSize: 20,
        opacity: 0,
        color: theme.palette.grey["500"],
    },
    totalHelperIcon: {
        position: "absolute",
        right: -20 - theme.spacing(0.5),
        fontSize: 20,
        opacity: 0,
        color: theme.palette.grey["500"],
    },
    warningIcon: {
        fontSize: 16,
        marginRight: theme.spacing(0.5),
        color: theme.palette.warning.main
    },
    issuesHelperIcon: {
        marginLeft: theme.spacing(0.5),
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
            onConfirm: () =>
                actions.loadableActions.deleteLanguage.dispatch({
                    path:
                        {
                            languageId: props.row.id,
                            repositoryId: repository.id
                        }
                }),
            title: <T>import_delete_language_dialog_title</T>,
            message: <T parameters={{languageName: props.row.name}}>import_delete_language_dialog_message</T>
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
                    {props.row.importFileName} ({props.row.name})
                    {props.row.importFileIssueCount ?
                        <Link
                            href="#"
                            onClick={() => {
                                props.onShowFileIssues()
                            }}>
                            <Box display="flex" alignItems="center" pt={1}>
                                <Warning className={classes.warningIcon}/>
                                {props.row.importFileIssueCount}
                                <Visibility className={clsx(classes.helperIcon, classes.issuesHelperIcon)}/>
                            </Box>
                        </Link>
                        : <></>}
                </TableCell>
                <TableCell scope="row" align="center">
                    <Box position="relative" display="inline">
                        <Link href="#" onClick={() => {
                            props.onShowData()
                        }}>{props.row.totalCount}</Link>
                        <Visibility className={clsx(classes.helperIcon, classes.totalHelperIcon)}/>
                    </Box>
                </TableCell>
                <TableCell scope="row" align="center">
                    <Button disabled={props.row.conflictCount < 1} onClick={() => props.onResolveConflicts()} size="small" className={classes.resolveButton}>
                        <CheckCircle className={classes.resolvedIcon}/>
                        {props.row.resolvedCount} / {props.row.conflictCount}
                        {props.row.conflictCount > 0 && <EditIcon className={clsx(classes.pencil, classes.helperIcon)}/>}
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
