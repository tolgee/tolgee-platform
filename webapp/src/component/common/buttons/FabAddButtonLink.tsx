import Fab from '@material-ui/core/Fab';
import AddIcon from '@material-ui/icons/Add';
import * as React from 'react';
import {Link} from 'react-router-dom';
import {useTranslate} from "@polygloat/react";


export function FabAddButtonLink(props: { to: string }) {
    const t = useTranslate();

    return <Fab color="primary" aria-label={t("button_add_aria_label", undefined, true)} component={Link} {...props}>
        <AddIcon/>
    </Fab>;
}
