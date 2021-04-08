import {Switch} from 'react-router-dom';
import * as React from 'react';
import {PrivateRoute} from "../../common/PrivateRoute";
import {OrganizationsListView} from "./OrganizationListView";
import {OrganizationCreateView} from "./OrganizationCreateView";
import {LINKS} from "../../../constants/links";
import {OrganizationProfileView} from "./OrganizationProfileView";
import {BoxLoading} from "../../common/BoxLoading";
import {DashboardPage} from "../../layout/DashboardPage";
import {OrganizationMembersView} from "./OrganizationMembersView";
import {useOrganization} from "../../../hooks/organizations/useOrganization";
import {OrganizationMemberPrivilegesView} from "./OrganizationMemberPrivilegesView";
import {OrganizationInvitationsView} from "./OrganizationInvitationsView";
import {container} from "tsyringe";
import {OrganizationActions} from "../../../store/organization/OrganizationActions";


const SpecificOrganizationRouter = () => {
    const organization = useOrganization()


    return (
        <DashboardPage>
            {organization ?
                <>
                    <PrivateRoute exact path={LINKS.ORGANIZATION_PROFILE.template}>
                        <OrganizationProfileView/>
                    </PrivateRoute>
                    <PrivateRoute exact path={LINKS.ORGANIZATION_MEMBERS.template}>
                        <OrganizationMembersView/>
                    </PrivateRoute>
                    <PrivateRoute exact path={LINKS.ORGANIZATION_MEMBER_PRIVILEGES.template}>
                        <OrganizationMemberPrivilegesView/>
                    </PrivateRoute>
                    <PrivateRoute exact path={LINKS.ORGANIZATION_INVITATIONS.template}>
                        <OrganizationInvitationsView/>
                    </PrivateRoute>
                </>
                :
                <BoxLoading/>
            }
        </DashboardPage>

    )
}

export const OrganizationsRouter = () => {
        return (
            <Switch>
                <PrivateRoute exact path={LINKS.ORGANIZATIONS.template}>
                    <OrganizationsListView/>
                </PrivateRoute>

                <PrivateRoute exact path={LINKS.ORGANIZATIONS_ADD.template}>
                    <OrganizationCreateView/>
                </PrivateRoute>

                <PrivateRoute path={LINKS.ORGANIZATION.template}>
                    <SpecificOrganizationRouter/>
                </PrivateRoute>
            </Switch>
        );
    }
;
