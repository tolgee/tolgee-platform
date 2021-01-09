import * as React from 'react';
import {FunctionComponent, useContext} from 'react';
import {KeyTranslationsDTO} from "../../service/response.types";
import {Box, Checkbox} from "@material-ui/core";
import {TableCell} from "./TableCell";
import {KeyCell} from "./KeyCell";
import {TranslationCell} from "./TranslationCell";
import {grey} from "@material-ui/core/colors";
import {TranslationListContext} from "./TtranslationsGridContextProvider";
import {createStyles, makeStyles} from "@material-ui/core/styles";
import {KeyScreenshots} from "./Screenshots/KeySreenshots";


export interface TranslationProps {
    data: KeyTranslationsDTO
}

export type RowContextType = {
    data: KeyTranslationsDTO,
    lastRendered: number,
}

export const RowContext = React.createContext<RowContextType>({data: null, lastRendered: 0});

const useStyles = makeStyles(() => createStyles({
    moreButton: {
        opacity: "0.8",
        padding: 0,
    },
    lineBox: {
        borderBottom: "1px solid " + grey[100],
        '&:last-child': {
            borderBottom: "none"
        }
    }
}));

export const TranslationsRow: FunctionComponent<TranslationProps> = (props) => {
    const classes = useStyles({});

    const listContext = useContext(TranslationListContext);

    const contextValue: RowContextType = {
        lastRendered: 0,
        data: props.data,
    };

    return (
        <Box display="flex" className={classes.lineBox}>
            <RowContext.Provider value={contextValue}>
                {listContext.showCheckBoxes &&
                <Box display="flex" alignItems="center" justifyContent="start" style={{width: 40}}>
                    <Checkbox onChange={() => listContext.toggleKeyChecked(contextValue.data.id)}
                              checked={listContext.isKeyChecked(contextValue.data.id)} size="small" style={{padding: 0}}/>
                </Box>}
                <Box display="flex" flexGrow={1} minWidth={0}>
                    {listContext.showKeys &&
                    <TableCell>
                        <KeyCell/>
                    </TableCell>}


                    {listContext.listLanguages.map(k =>
                        <TableCell key={k}>
                            <TranslationCell abbreviation={k}/>
                        </TableCell>
                    )}
                </Box>
                <KeyScreenshots data={props.data}/>
            </RowContext.Provider>
        </Box>
    )
};