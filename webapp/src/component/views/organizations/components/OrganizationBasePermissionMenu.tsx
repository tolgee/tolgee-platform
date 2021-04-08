import {Box, Button, Grid, Menu, MenuItem} from "@material-ui/core";
import {ArrowDropDown} from "@material-ui/icons";
import {T} from "@tolgee/react";
import * as React from "react";
import {useUser} from "../../../../hooks/useUser";
import {components} from "../../../../service/apiSchema";
import {FunctionComponent} from "react";
import {OrganizationRoleType, RepositoryPermissionType} from "../../../../service/response.types";
import {container} from "tsyringe";
import {OrganizationActions} from "../../../../store/organization/OrganizationActions";
import {useOrganization} from "../../../../hooks/organizations/useOrganization";
import {confirmation} from "../../../../hooks/confirmation";

const actions = container.resolve(OrganizationActions)

export const OrganizationBasePermissionMenu: FunctionComponent<{ organization: components["schemas"]["OrganizationModel"] }> = (props) => {
    const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);

    const organization = useOrganization();

    const handleClose = () => {
        setAnchorEl(null);
    };

    const handleSet = (type: components["schemas"]["OrganizationDto"]["basePermissions"]) => {
        confirmation({
            message: <T>really_want_to_change_base_permission_confirmation</T>,
            hardModeText: organization.name,
            onConfirm: () => {
                const {_links, id, ...dto} = organization
                dto.basePermissions = type
                actions.loadableActions.edit.dispatch(id, dto)
            }
        })
        handleClose();
    };

    const handleClick = (event: React.MouseEvent<HTMLButtonElement>) => {
        setAnchorEl(event.currentTarget);
    };


    return (
        <>
            <Button variant="outlined" aria-controls="simple-menu" aria-haspopup="true"
                    onClick={handleClick}>
                <T>{`permission_type_${props.organization.basePermissions.toLowerCase()}`}</T> <ArrowDropDown fontSize="small"/>
            </Button>
            <Menu
                elevation={1}
                id="simple-menu"
                anchorEl={anchorEl}
                getContentAnchorEl={null}
                keepMounted
                open={Boolean(anchorEl)}
                onClose={handleClose}
                anchorOrigin={{
                    vertical: "bottom",
                    horizontal: 'center'
                }}
                transformOrigin={{
                    vertical: 'top',
                    horizontal: 'right',
                }}
            >
                {Object.keys(RepositoryPermissionType).map(k =>
                    <MenuItem
                        key={k}
                        onClick={() => handleSet(k as any)}
                        selected={k === props.organization.basePermissions}
                    >
                        <T>{`permission_type_${k.toLowerCase()}`}</T>
                    </MenuItem>)}
            </Menu>
        </>
    )
}
