import {Switch, useRouteMatch} from 'react-router-dom';
import * as React from 'react';
import {RepositoryListView} from './RepositoryListView';
import {RepositoryCreateView} from './repository/RepositoryCreateView';
import {LINKS} from '../../../constants/links';
import {PrivateRoute} from "../../common/PrivateRoute";
import {RepositoryRouter} from "./RepositoryRouter";

export const RepositoriesRouter = () => {
    let match = useRouteMatch();

    return (
        <Switch>
            <PrivateRoute exact path={`${match.path}`}>
                <RepositoryListView/>
            </PrivateRoute>

            <PrivateRoute exact path={`${LINKS.REPOSITORY_ADD.template}`}>
                <RepositoryCreateView/>
            </PrivateRoute>

            <PrivateRoute path={LINKS.REPOSITORY.template}>
                <RepositoryRouter/>
            </PrivateRoute>

        </Switch>
    );
};
