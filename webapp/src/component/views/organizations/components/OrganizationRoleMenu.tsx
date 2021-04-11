import {Box, Button, Grid, Menu, MenuItem} from "@material-ui/core";
import {ArrowDropDown} from "@material-ui/icons";
import {T} from "@tolgee/react";
import * as React from "react";
import {useUser} from "../../../../hooks/useUser";
import {components} from "../../../../service/apiSchema";
import {FunctionComponent} from "react";
import {OrganizationRoleType} from "../../../../service/response.types";
import {container} from "tsyringe";
import {OrganizationActions} from "../../../../store/organization/OrganizationActions";
import {useOrganization} from "../../../../hooks/organizations/useOrganization";
import {confirmation} from "../../../../hooks/confirmation";

const actions = container.resolve(OrganizationActions)

export const OrganizationRoleMenu: FunctionComponent<{ user: components["schemas"]["UserAccountWithOrganizationRoleModel"] }> = (props) => {
    const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);

    const currentUser = useUser();
    const organization = useOrganization();

    const handleClose = () => {
        setAnchorEl(null);
    };

    const handleSet = (type: string) => {
        confirmation({
            message: <T>really_want_to_change_role_confirmation</T>,
            onConfirm: () => actions.loadableActions.setRole.dispatch(organization.id, props.user!.id, type)

        })

        handleClose();
    };

    const handleClick = (event: React.MouseEvent<HTMLButtonElement>) => {
        setAnchorEl(event.currentTarget);
    };


    return (
        <>
            <Button disabled={currentUser?.id == props.user.id} variant="outlined" size="small" aria-controls="simple-menu" aria-haspopup="true"
                    onClick={handleClick}>
                <T>{`organization_role_type_${props.user.organizationRole}`}</T> <ArrowDropDown fontSize="small"/>
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
                {Object.keys(OrganizationRoleType).map(k =>
                    <MenuItem
                        key={k}
                        onClick={() => handleSet(k)}
                        selected={k === props.user.organizationRole}
                    >
                        <T>{`organization_role_type_${k}`}</T>
                    </MenuItem>)}
            </Menu>
        </>
    )
}
