import * as React from 'react';
import {FunctionComponent, useContext} from 'react';
import {Box} from "@material-ui/core";
import {RowContext} from "./TranslationsRow";
import {TranslationListContext} from "./TtranslationsGridContextProvider";

export interface TranslationsTableCellProps {

}

export const TableCell: FunctionComponent<TranslationsTableCellProps> = (props) => {

    let rowContext = useContext(RowContext);
    let listContext = useContext(TranslationListContext);

    const width = listContext.cellWidths[rowContext.lastRendered];

    rowContext.lastRendered++;

    return (
        <Box width={width + "%"} p={0.5} display="flex" alignItems="center">
            {props.children}
        </Box>
    )
};