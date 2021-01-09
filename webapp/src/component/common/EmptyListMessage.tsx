import {default as React, FunctionComponent} from 'react';

import {SadGoatMessage} from "./SadGoatMessage";
import {Box} from "@material-ui/core";
import {T} from '@polygloat/react';

export const EmptyListMessage: FunctionComponent = (props) => {
    return <Box p={8}><SadGoatMessage>{props.children || <T>global_empty_list_message</T>}</SadGoatMessage></Box>
};