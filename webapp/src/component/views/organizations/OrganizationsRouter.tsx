import {Switch, useRouteMatch} from 'react-router-dom';
import * as React from 'react';
import {PrivateRoute} from "../../common/PrivateRoute";
import {OrganizationsListView} from "./OrganizationListView";
import {OrganizationCreateView} from "./OrganizationCreateView";
import {LINKS} from "../../../constants/links";

export const OrganizationsRouter = () => {
    let match = useRouteMatch();

    return (
        <Switch>
            <PrivateRoute exact path={`${match.path}`}>
                <OrganizationsListView/>
            </PrivateRoute>
            <PrivateRoute exact path={LINKS.ORGANIZATIONS_ADD.template}>
                <OrganizationCreateView/>
            </PrivateRoute>
        </Switch>
    );
};
