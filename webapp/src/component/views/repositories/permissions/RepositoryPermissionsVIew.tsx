import {default as React, FunctionComponent, useEffect, useState} from 'react';
import {useRouteMatch} from 'react-router-dom';
import {PARAMS} from '../../../../constants/links';
import {Button} from '@material-ui/core';
import {BaseView} from '../../../layout/BaseView';
import {useSelector} from 'react-redux';
import {AppState} from '../../../../store';
import {container} from 'tsyringe';
import {repositoryPermissionTypes} from '../../../../constants/repositoryPermissionTypes';
import List from '@material-ui/core/List';
import ListItem from '@material-ui/core/ListItem';
import ListItemText from '@material-ui/core/ListItemText';
import ListItemSecondaryAction from '@material-ui/core/ListItemSecondaryAction';
import {RepositoryPermissionActions} from '../../../../store/repository/invitations/repositoryPermissionActions';
import EditIcon from '@material-ui/icons/Edit';
import {MicroForm} from '../../../common/form/MicroForm';
import CloseIcon from '@material-ui/icons/Close';
import CheckIcon from '@material-ui/icons/Check';
import {confirmation} from '../../../../hooks/confirmation';
import {useUser} from "../../../../hooks/useUser";
import {PermissionEditDTO} from "../../../../service/response.types";
import {T} from "@polygloat/react";
import {PermissionSelect} from "../../../security/PermissionSelect";
import {ConfirmationDialogProps} from "../../../common/ConfirmationDialog";

export const RepositoryPermissionsView: FunctionComponent = () => {

    const actions = container.resolve(RepositoryPermissionActions);

    let match = useRouteMatch();
    const repositoryId = match.params[PARAMS.REPOSITORY_ID];

    let state = useSelector((state: AppState) => state.repositoryPermission);

    useEffect(() => {
        if (!state.loadables.list.loading) {
            actions.loadableActions.list.dispatch(repositoryId);
        }
    }, [state.loadables.list.loaded]);

    let confirmationMessage = (options: ConfirmationDialogProps) => confirmation({title: 'Revoke access', ...options});

    const userData = useUser();

    const [editingId, setEditingId] = useState(null);

    return (
        <BaseView title={<T>edit_repository_permissions_title</T>} xs={12} md={10} lg={8}
                  loading={state.loadables.list.loading}>
            {() => (
                <List>
                    {state.loadables.list.data && state.loadables.list.data.map(p => (
                        <ListItem key={p.id}>
                            <MicroForm initialValues={{type: p.type}} onSubmit={(v) => {
                                actions.loadableActions.edit.dispatch({permissionId: p.id, type: v.type} as PermissionEditDTO);
                                setEditingId(null);
                            }}>
                                <ListItemText>
                                    {p.userFullName} | {p.username}
                                    &nbsp;[ <i><T>repository_permission_label</T> {p.id === editingId &&
                                <PermissionSelect name="type" className={null}/> || <T>{`permission_type_${repositoryPermissionTypes[p.type]}`}</T>}</i> ]
                                </ListItemText>
                                {userData && userData.id !== p.userId &&
                                <ListItemSecondaryAction>
                                    {p.id === editingId &&
                                    <>
                                        <Button color="primary" type="submit">
                                            <CheckIcon/>
                                        </Button>
                                        <Button onClick={() => setEditingId(null)}><CloseIcon/></Button>
                                    </>
                                    ||
                                    <>
                                        <Button color="primary" onClick={(e) => {
                                            e.preventDefault();
                                            setEditingId(p.id)
                                        }}>
                                            <EditIcon/>
                                        </Button>
                                        <Button color="secondary" onClick={
                                            () => confirmationMessage({
                                                message: `Do you really want to revoke access for user ${p.userFullName}?`,
                                                onConfirm: () => actions.loadableActions.delete.dispatch(p.id)
                                            })
                                        }>Revoke</Button>
                                    </>
                                    }
                                </ListItemSecondaryAction>}
                            </MicroForm>
                        </ListItem>
                    ))}
                </List>
            )}
        </BaseView>
    );
};
