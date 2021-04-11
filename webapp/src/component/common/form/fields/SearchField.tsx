import React, {ComponentProps, useEffect, useState} from 'react';
import {InputAdornment, TextField} from "@material-ui/core";
import {Search} from "@material-ui/icons";
import { T } from '@tolgee/react';

const SearchField = (props: {
    initial?: string,
    onSearch: (value: string) => void
} & ComponentProps<typeof TextField>) => {
    const [search, setSearch] = useState(props.initial || "");
    const [oldSearch, setOldSearch] = useState("");

    const {onSearch, ...otherProps} = props

    useEffect(() => {
        const handler = setTimeout(() => {
            if (oldSearch !== search) {
                onSearch(search)
                setOldSearch(search);
            }
        }, 500);
        return () => clearTimeout(handler);
    }, [search]);

    return (
        <TextField type="search"
                   label={<T>standard_search_label</T>}
                   InputProps={{
                       startAdornment: (
                           <InputAdornment position="start">
                               <Search/>
                           </InputAdornment>)
                   }}
                   value={search}
                   {...otherProps}
                   onChange={(e) => setSearch(e.target.value)}/>
    );
};

export default SearchField;
