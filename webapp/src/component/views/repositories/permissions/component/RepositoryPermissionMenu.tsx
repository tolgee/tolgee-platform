import React from 'react';
import {PermissionsMenu} from "../../../../security/PermissionsMenu";
import {useRepository} from "../../../../../hooks/useRepository";
import {components} from "../../../../../service/apiSchema";
import {useUser} from "../../../../../hooks/useUser";
import {container} from "tsyringe";
import {RepositoryActions} from "../../../../../store/repository/RepositoryActions";
import {confirmation} from "../../../../../hooks/confirmation";
import {T} from '@tolgee/react';

const repositoryActions = container.resolve(RepositoryActions);
const RepositoryPermissionMenu = (props: { user: components["schemas"]["UserAccountInRepositoryModel"] }) => {

    const repository = useRepository()
    const currentUser = useUser()

    return (
        <PermissionsMenu
            selected={props.user.computedPermissions || repository.organizationOwnerBasePermissions!}
            onSelect={(permissionType) => {
                confirmation({
                    message: <T>change_permissions_confirmation</T>,
                    onConfirm: () => repositoryActions.loadableActions.setUsersPermissions.dispatch({
                        path: {
                            userId: props.user?.id!!,
                            permissionType,
                            repositoryId: repository.id
                        }
                    })
                })
            }}
            buttonProps={{size: "small", disabled: currentUser?.id === props.user.id}}
            minPermissions={props.user.organizationBasePermissions}
        />
    );
};

export default RepositoryPermissionMenu;
