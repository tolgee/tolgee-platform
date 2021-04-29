import React, {FunctionComponent} from 'react';
import {components} from "../../../../../service/apiSchema";
import {Box, makeStyles, Table, TableBody, TableCell, TableContainer, TableHead} from "@material-ui/core";
import {T} from "@tolgee/react";
import {ImportResultRow} from "./ImportResultRow";

type ImportResultProps = {
    result?: components["schemas"]["PagedModelImportLanguageModel"]
}

const useStyles = makeStyles(theme => ({
    table: {
        "& th": {
            fontWeight: "bold"
        }
    }
}))

export const ImportResult: FunctionComponent<ImportResultProps> = (props) => {
    const classes = useStyles()

    const rows = props.result?._embedded?.languages

    if (!rows) {
        return <></>
    }

    return (
        <Box mt={5}>
            <TableContainer>
                <Table className={classes.table}>
                    <TableHead>
                        <TableCell>
                            <T>import_result_language_name_header</T>
                        </TableCell>
                        <TableCell>
                            <T>import_result_file_name_header</T>
                        </TableCell>
                        <TableCell>
                            <T>import_result_total_count_header</T>
                        </TableCell>
                        <TableCell>
                            <T>import_result_total_conflict_count_header</T>
                        </TableCell>
                        <TableCell>
                        </TableCell>
                    </TableHead>
                    <TableBody>
                        {rows.map(row => <ImportResultRow key={row.id} row={row}/>)}
                    </TableBody>
                </Table>
            </TableContainer>
        </Box>
    );
};
