import * as React from 'react';
import {FunctionComponent, useContext, useEffect, useState} from 'react';
import {TextField} from "@material-ui/core";
import {TranslationListContext} from "./TtranslationsGridContextProvider";
import {T} from "@tolgee/react";

export const SearchField: FunctionComponent = (props) => {
    const listContext = useContext(TranslationListContext);

    const [search, setSearch] = useState(listContext.listLoadable.data ? listContext.listLoadable.data.params.search || "" : "");
    const [oldSearch, setOldSearch] = useState("");

    useEffect(() => {
        const handler = setTimeout(() => {
            if (oldSearch !== search) {
                listContext.loadData(search);
                setOldSearch(search);
            }
        }, 500);
        return () => clearTimeout(handler);
    }, [search]);


    return (
        <TextField id="standard-search"
                   label={<T>translations_search_field_label</T>}
                   type="search"
                   value={search}
                   onChange={(e) => setSearch(e.target.value)}/>
    );
};