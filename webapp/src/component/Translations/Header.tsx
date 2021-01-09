import * as React from 'react';
import {FunctionComponent, useContext} from 'react';
import {Box, Checkbox} from "@material-ui/core";
import {TableCell} from "./TableCell";
import {RowContext} from "./TranslationsRow";
import {TranslationListContext} from "./TtranslationsGridContextProvider";

export const Header: FunctionComponent = () => {

    const listContext = useContext(TranslationListContext);

    return (
        <Box display="flex" height={40}>
            <RowContext.Provider value={{data: null, lastRendered: 0}}>
                {listContext.showCheckBoxes &&
                <Box width={40} display="flex" alignItems="center">
                    <Checkbox checked={listContext.isAllChecked()}
                              indeterminate={!listContext.isAllChecked() && listContext.isSomeChecked()}
                              onChange={() => listContext.checkAllToggle()} style={{padding: 0}} size="small"/>
                </Box>}
                <Box display="flex" flexGrow={1}>
                    {listContext.headerCells.map((inner, key) =>
                        <TableCell key={key}>
                            {inner}
                        </TableCell>
                    )}
                </Box>
                <Box width={"24px"}/>
                {/*The size of advanced view icon in rows*/}
                {/*<Box width={"24px"}>*/}
                {/*</Box>*/}
            </RowContext.Provider>
        </Box>
    )
};