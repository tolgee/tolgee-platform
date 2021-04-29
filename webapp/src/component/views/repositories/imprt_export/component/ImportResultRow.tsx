import {components} from "../../../../../service/apiSchema";
import React from "react";
import {Box, Button, Collapse, IconButton, TableCell, TableRow} from "@material-ui/core";
import DeleteIcon from "@material-ui/icons/Delete";

export const ImportResultRow = (props: { row: components["schemas"]["ImportLanguageModel"] }) => {
    const [open, setOpen] = React.useState(false);

    const expandAllTranslations = () => {

    }

    const expandConflicts = () => {

    }

    return (
        <React.Fragment>
            <TableRow>
                <TableCell scope="row">
                    {props.row.existingLanguageName}
                </TableCell>
                <TableCell scope="row">
                    {props.row.importFileName}
                </TableCell>
                <TableCell scope="row">
                    <Button onClick={expandAllTranslations} size="small">{props.row.totalCount}</Button>
                </TableCell>
                <TableCell scope="row">
                    <Button onClick={expandConflicts} size="small">{props.row.conflictCount}</Button>
                </TableCell>
                <TableCell scope="row" align={"right"}>
                    <IconButton size="small" style={{padding: 0}}>
                        <DeleteIcon/>
                    </IconButton>
                </TableCell>
            </TableRow>
            <TableRow>
                <TableCell style={{paddingBottom: 0, paddingTop: 0}} colSpan={6}>
                    <Collapse in={open} timeout="auto" unmountOnExit>
                        <Box margin={1}>

                        </Box>
                    </Collapse>
                </TableCell>
            </TableRow>
        </React.Fragment>
    );
}
