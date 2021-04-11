import {default as React, FunctionComponent} from 'react';
import {useRouteMatch} from 'react-router-dom';
import {PARAMS} from '../../../../constants/links';
import {BaseView} from '../../../layout/BaseView';
import {useSelector} from 'react-redux';
import {AppState} from '../../../../store';
import {container} from 'tsyringe';
import {T} from "@tolgee/react";
import {SimplePaginatedHateoasList} from "../../../common/list/SimplePaginatedHateoasList";
import {RepositoryActions} from "../../../../store/repository/RepositoryActions";
import {SimpleListItem} from "../../../common/list/SimpleListItem";
import {Box, Chip, ListItemSecondaryAction, ListItemText, Typography} from "@material-ui/core";
import RevokePermissionsButton from "./component/RevokePermissionsButton";
import {useRepository} from "../../../../hooks/useRepository";
import {translatedPermissionType} from "../../../../fixtures/translatePermissionFile";
import RepositoryPermissionMenu from "./component/RepositoryPermissionMenu";

const repositoryActions = container.resolve(RepositoryActions);

export const RepositoryPermissionsView: FunctionComponent = () => {
    const repository = useRepository()

    let listLoadable = useSelector((state: AppState) => state.repositories.loadables.listUsersForPermissions);

    const basePermissionText = translatedPermissionType(repository.organizationOwnerBasePermissions!, true);

    return (
        <BaseView title={<T>edit_repository_permissions_title</T>} xs={12} md={10} lg={8}
                  loading={listLoadable.loading} hideChildrenOnLoading={false}>

            {repository.organizationOwnerAddressPart &&
            <Box mb={2}>
                <Typography variant={"body1"}><T>repository_permission_information_text_base_permission_before</T><Chip
                    label={basePermissionText}/></Typography>

                <T>repository_permission_information_text_base_permission_after</T>

            </Box>}

            <SimplePaginatedHateoasList actions={repositoryActions}
                                        dispatchParams={[repository.id, undefined]}
                                        loadableName="listUsersForPermissions"
                                        renderItem={u =>
                                            <SimpleListItem>
                                                <ListItemText>
                                                    {u.name} ({u.username}) {u.organizationRole && <Chip size="small" label={repository.organizationOwnerName}/>}
                                                </ListItemText>
                                                <ListItemSecondaryAction>
                                                    <Box mr={1} display="inline">
                                                        <RepositoryPermissionMenu user={u}/>
                                                    </Box>
                                                    <RevokePermissionsButton user={u}/>
                                                </ListItemSecondaryAction>
                                            </SimpleListItem>
                                        }
            />
        </BaseView>
    );
}
