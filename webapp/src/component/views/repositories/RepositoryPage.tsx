import * as React from 'react';
import {FunctionComponent} from 'react';

import {DashboardPage} from '../../layout/DashboardPage';
import {useRepository} from "../../../hooks/useRepository";
import {RepositoryMenu} from "./RepositoryMenu";

interface Props {
    fullWidth?: boolean
}


export const RepositoryPage: FunctionComponent<Props> = (props) => {
    const repository = useRepository();

    return (
        <DashboardPage fullWidth={props.fullWidth} repositoryName={repository.name} sideMenuItems={<RepositoryMenu id={repository.id}/>}>
            {props.children}
        </DashboardPage>
    );
};
