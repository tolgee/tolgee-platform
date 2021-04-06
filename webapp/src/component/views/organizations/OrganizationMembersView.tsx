import * as React from 'react';
import {FunctionComponent} from 'react';
import {container} from 'tsyringe';
import {T} from "@tolgee/react";
import {OrganizationActions} from "../../../store/organization/OrganizationActions";
import {BaseOrganizationSettingsView} from "./BaseOrganizationSettingsView";
import {SimplePaginatedHateoasList} from "../../common/list/SimplePaginatedHateoasList";
import {Box, Button, Grid, Menu, MenuItem, Theme, Typography} from "@material-ui/core";
import {ArrowDropDown} from "@material-ui/icons";
import makeStyles from "@material-ui/core/styles/makeStyles";
import createStyles from "@material-ui/core/styles/createStyles";
import {useUser} from "../../../hooks/useUser";
import {useOrganization} from "../../../hooks/organizations/useOrganization";
import {useLeaveOrganization} from "../../../hooks/organizations/useLeaveOrganization";
import {ResourceErrorComponent} from "../../common/form/ResourceErrorComponent";

const actions = container.resolve(OrganizationActions);

const useStyles = makeStyles((theme: Theme) =>
    createStyles({
        container: {
            borderBottom: `1px solid ${theme.palette.grey.A100}`,
            "&:last-child": {
                borderBottom: `none`,
            }
        }
    }),
);

export const OrganizationMembersView: FunctionComponent = () => {
    const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);

    const organization = useOrganization()

    const handleClose = () => {
        setAnchorEl(null);
    };

    const handleClick = (event: React.MouseEvent<HTMLButtonElement>) => {
        setAnchorEl(event.currentTarget);
    };

    const classes = useStyles();

    const currentUser = useUser();

    const [leaveLoadable, leaveOrganization] = useLeaveOrganization()

    return (
        <BaseOrganizationSettingsView title={<T>organization_members_view_title</T>}>
            <ResourceErrorComponent error={leaveLoadable.error}/>
            <SimplePaginatedHateoasList
                renderItem={(i) =>
                    <Box p={1} pl={2} className={classes.container}>
                        <Grid container justify="space-between" alignItems="center">
                            <Grid item data-cy={"organizations-user-name"} lg={3} md={3} sm={6}>
                                <Box mr={1}>
                                    <Typography variant={"body1"} noWrap>{i.name}</Typography>
                                </Box>
                            </Grid>
                            <Grid item data-cy={"organizations-user-email"} lg={5} md={5} sm={6}>
                                <Typography variant={"body1"} noWrap>{i.username}</Typography>
                            </Grid>
                            <Grid item lg={4} md={4} style={{display: "flex", justifyContent: "flex-end"}}>
                                <Button disabled={currentUser?.id == i.id} variant="outlined" size="small" aria-controls="simple-menu" aria-haspopup="true"
                                        onClick={handleClick}>
                                    <T>{`organization_role_type_${i.organizationRoleType}`}</T> <ArrowDropDown fontSize="small"/>
                                </Button>
                                <Box display={"inline"} ml={2}>
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
                                        <MenuItem onClick={handleClose}><T>organization_role_type_MEMBER</T></MenuItem>
                                        <MenuItem onClick={handleClose}><T>organization_role_type_OWNER</T></MenuItem>
                                    </Menu>
                                    {currentUser?.id == i.id ?
                                        <Button
                                            variant="outlined"
                                            size="small"
                                            aria-controls="simple-menu"
                                            aria-haspopup="true"
                                            onClick={() => leaveOrganization(organization.id)}
                                        >
                                            <T>organization_users_leave</T>
                                        </Button>
                                        :
                                        <Button variant="outlined" size="small" aria-controls="simple-menu" aria-haspopup="true">
                                            <T>organization_users_remove_user</T>
                                        </Button>}
                                </Box>
                            </Grid>
                        </Grid>
                    </Box>
                }
                actions={actions}
                loadableName="listAllUsers"
                dispatchParams={[organization.id]}
            />


        </BaseOrganizationSettingsView>
    );
}
