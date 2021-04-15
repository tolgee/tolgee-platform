import React, {FunctionComponent, ReactElement} from 'react';
import {Button, Tooltip} from "@material-ui/core";
import {confirmation} from "../../../../../hooks/confirmation";
import {T} from '@tolgee/react';
import {components} from "../../../../../service/apiSchema";
import {useUser} from "../../../../../hooks/useUser";
import {container} from "tsyringe";
import {RepositoryActions} from "../../../../../store/repository/RepositoryActions";
import {useRepository} from "../../../../../hooks/useRepository";

const repositoryActions = container.resolve(RepositoryActions);

const RevokePermissionsButton = (props: { user: components["schemas"]["UserAccountInRepositoryModel"] }) => {

    const hasOrganizationRole = !!props.user.organizationRole
    const repository = useRepository()
    const currentUser = useUser();
    let disabledTooltipTitle = undefined as ReactElement | undefined;


    if (currentUser!.id === props.user.id) {
        disabledTooltipTitle = <T noWrap>cannot_revoke_your_own_access_tooltip</T>
    } else if (hasOrganizationRole) {
        disabledTooltipTitle = <T noWrap>user_is_part_of_organization_tooltip</T>
    }

    const isDisabled = !!disabledTooltipTitle

    const Wrapper: FunctionComponent = (props) => !isDisabled ? <>{props.children}</> :
        <Tooltip title={disabledTooltipTitle!}><span>{props.children}</span></Tooltip>

    return (
        <Wrapper>
            <Button
                data-cy="permissions-revoke-button"
                disabled={isDisabled} size="small" variant="outlined" onClick={
                () => confirmation({
                    title: <T>revoke_access_confirmation_title</T>,
                    message: <T parameters={{userName: props.user.name}}>repository_permissions_revoke_user_access_message</T>,
                    onConfirm: () => {
                        repositoryActions.loadableActions.revokeAccess.dispatch({
                            path: {
                                repositoryId: repository.id,
                                userId: props.user.id
                            }
                        })
                    }
                })
            }>Revoke</Button>
        </Wrapper>
    );
};

export default RevokePermissionsButton;
