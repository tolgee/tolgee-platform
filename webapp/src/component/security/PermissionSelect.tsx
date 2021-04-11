import {Select} from "../common/form/fields/Select";
import {repositoryPermissionTypes} from "../../constants/repositoryPermissionTypes";
import {ComponentProps, default as React, FunctionComponent} from "react";
import {MenuItem} from "@material-ui/core";
import {translatedPermissionType} from "../../fixtures/translatePermissionFile";

export const PermissionSelect: FunctionComponent<ComponentProps<typeof Select>> = (props) => {
    return <Select {...props}
                   renderValue={v => translatedPermissionType(v)}>
        {Object.keys(repositoryPermissionTypes).map(k =>
            <MenuItem key={k} value={k}>{translatedPermissionType(k)}</MenuItem>)}
    </Select>
}
