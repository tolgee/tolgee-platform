import * as React from 'react';
import {FunctionComponent} from 'react';
import {container} from 'tsyringe';
import {T} from "@tolgee/react";
import {OrganizationActions} from "../../../store/organization/OrganizationActions";
import {BaseOrganizationSettingsView} from "./BaseOrganizationSettingsView";
import {SimplePaginatedHateoasList} from "../../common/list/SimplePaginatedHateoasList";
import {Box, Button, Grid, Theme, Typography} from "@material-ui/core";
import makeStyles from "@material-ui/core/styles/makeStyles";
import createStyles from "@material-ui/core/styles/createStyles";
import {useUser} from "../../../hooks/useUser";
import {useOrganization} from "../../../hooks/organizations/useOrganization";
import {useLeaveOrganization} from "../../../hooks/organizations/useLeaveOrganization";
import {OrganizationRoleMenu} from "./components/OrganizationRoleMenu";
import OrganizationRemoveUserButton from "./components/OrganizationRemoveUserButton";

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

    const organization = useOrganization();

    const classes = useStyles();

    const currentUser = useUser();

    const [leaveOrganization, leaveOrganizationError] = useLeaveOrganization()

    return (
        <BaseOrganizationSettingsView>
            {leaveOrganizationError}
            <SimplePaginatedHateoasList
                search
                title={<T>organization_members_view_title</T>}
                pageSize={10}
                renderItem={(i) =>
                    <Box p={1} pl={2} className={classes.container} key={i.id}>
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
                                <OrganizationRoleMenu user={i}/>
                                <Box display={"inline"} ml={1}>
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
                                        <OrganizationRemoveUserButton userId={i.id} userName={i.name}/>
                                    }
                                </Box>
                            </Grid>
                        </Grid>
                    </Box>
                }
                actions={actions}
                loadableName="listUsers"
                dispatchParams={[{
                    path: {id: organization.id}
                }]}
            />


        </BaseOrganizationSettingsView>
    );
}
