import * as React from 'react';
import {FunctionComponent} from 'react';
import {useSelector} from 'react-redux';
import {AppState} from "../../store";
import {RepositoryPage} from "./repositories/RepositoryPage";
import {DashboardPage} from "../layout/DashboardPage";


export const PossibleRepositoryPage: FunctionComponent = (props) => {

    let repository = useSelector((state: AppState) => state.repositories.loadables.repository.loaded);

    return (
        repository ? <RepositoryPage {...props}/> : <DashboardPage {...props}/>
    );
};

