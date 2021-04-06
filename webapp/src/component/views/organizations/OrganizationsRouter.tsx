import {Switch, useRouteMatch} from 'react-router-dom';
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


const SpecificOrganizationRouter = () => {
    const organization = useOrganization()

    return (
        <>
            {organization ?
                <>
                    <DashboardPage>

                        <PrivateRoute exact path={LINKS.ORGANIZATION_PROFILE.template}>
                            <OrganizationProfileView/>
                        </PrivateRoute>
                        <PrivateRoute exact path={LINKS.ORGANIZATION_MEMBERS.template}>
                            <OrganizationMembersView/>
                        </PrivateRoute>
                    </DashboardPage>
                </> :
                <BoxLoading/>}
        </>
    )
}

export const OrganizationsRouter = () =>
{
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
