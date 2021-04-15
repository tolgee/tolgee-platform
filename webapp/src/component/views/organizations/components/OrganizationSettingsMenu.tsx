import {MenuItem, MenuList, Paper} from "@material-ui/core";
import {Link} from "react-router-dom";
import * as React from "react";
import {T} from "@tolgee/react";
import {useOrganizationMenuItems} from "../../../../hooks/organizations/useOrganizationMenuItems";

export const OrganizationSettingsMenu = () => {
    const menuItems = useOrganizationMenuItems()

    return (
        <Paper elevation={0} variant="outlined" data-cy="organization-side-menu">
            <MenuList>
                {menuItems.map((mi, idx) =>
                    <MenuItem
                        key={idx}
                        selected={mi.isSelected}
                        component={Link}
                        to={mi.link}>
                        <T>{mi.nameTranslationKey}</T>
                    </MenuItem>)}
            </MenuList>
        </Paper>
    );
}
