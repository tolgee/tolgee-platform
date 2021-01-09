import {Select} from "../common/form/fields/Select";
import {repositoryPermissionTypes} from "../../constants/repositoryPermissionTypes";
import MenuItem from "@material-ui/core/MenuItem";
import {ComponentProps, default as React, FunctionComponent} from "react";
import {T} from "@polygloat/react";

export const PermissionSelect: FunctionComponent<ComponentProps<typeof Select>> = (props) => {
    return <Select {...props}
                   renderValue={v => <T>{`permission_type_${repositoryPermissionTypes[v]}`}</T>}>
        {Object.keys(repositoryPermissionTypes).map(k =>
            <MenuItem key={k} value={k}><T>{`permission_type_${repositoryPermissionTypes[k]}`}</T></MenuItem>)}
    </Select>
}