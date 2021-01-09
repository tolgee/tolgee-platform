import * as React from 'react';
import {useEffect} from 'react';
import Box from '@material-ui/core/Box';
import {connect} from 'react-redux';
import {AppState} from '../../../store';
import {container} from 'tsyringe';
import {RepositoryActions} from '../../../store/repository/RepositoryActions';
import {RepositoryDTO, RepositoryPermissionType} from '../../../service/response.types';
import {LINKS, PARAMS} from '../../../constants/links';
import {FabAddButtonLink} from '../../common/buttons/FabAddButtonLink';
import List from '@material-ui/core/List';
import {ListItemLink} from '../../common/list/ListItemLink';
import ListItemSecondaryAction from '@material-ui/core/ListItemSecondaryAction';
import ListItemText from '@material-ui/core/ListItemText';
import {SettingsIconButton} from '../../common/buttons/SettingsIconButton';
import {Link} from 'react-router-dom';
import {BaseView} from '../../layout/BaseView';
import {EmptyListMessage} from "../../common/EmptyListMessage";
import {PossibleRepositoryPage} from "../PossibleRepositoryPage";
import {useTranslate} from "@polygloat/react";

const actions = container.resolve(RepositoryActions);

interface Props {
    repositories: RepositoryDTO[];
    loading: boolean;
}

export const RepositoryListView = connect((state: AppState) =>
    ({repositories: state.repositories.repositories, loading: state.repositories.repositoriesLoading}))(
    ({repositories, loading}: Props) => {

        useEffect(() => {
            actions.loadRepositories.dispatch();
        }, []);

        const t = useTranslate();

        return (
            <PossibleRepositoryPage>
                <BaseView title={t("repositories_title")} lg={5} md={7} loading={loading}>
                    {() => (
                        <Box ml={-2}>
                            {repositories.length && <List>
                                {repositories.map(r =>
                                    <ListItemLink
                                        key={r.id}
                                        to={LINKS.REPOSITORY_TRANSLATIONS.build({[PARAMS.REPOSITORY_ID]: r.id})}>
                                        <ListItemText>
                                            {r.name}
                                        </ListItemText>
                                        {r.permissionType === RepositoryPermissionType.MANAGE &&
                                        <ListItemSecondaryAction>
                                            <Link to={LINKS.REPOSITORY_EDIT.build({[PARAMS.REPOSITORY_ID]: r.id})}>
                                                <SettingsIconButton/>
                                            </Link>
                                        </ListItemSecondaryAction>}
                                    </ListItemLink>)}
                            </List> || <EmptyListMessage/>}
                            <Box display="flex" flexDirection="column" alignItems="flex-end" mt={2} pr={2}>
                                <FabAddButtonLink to={LINKS.REPOSITORY_ADD.build()}/>
                            </Box>
                        </Box>
                    )}
                </BaseView>
            </PossibleRepositoryPage>
        );
    }
);

