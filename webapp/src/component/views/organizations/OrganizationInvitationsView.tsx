import * as React from 'react';
import {FunctionComponent, useEffect, useState} from 'react';
import {useSelector} from 'react-redux';
import {container} from 'tsyringe';
import {T, useTranslate} from "@tolgee/react";
import {OrganizationActions} from "../../../store/organization/OrganizationActions";
import {AppState} from "../../../store";
import {LINKS, PARAMS} from "../../../constants/links";
import {Redirect} from "react-router-dom";
import {components} from "../../../service/apiSchema";
import {Validation} from "../../../constants/GlobalValidationSchema";
import {OrganizationFields} from "./components/OrganizationFields";
import {MessageService} from "../../../service/MessageService";
import {StandardForm} from "../../common/form/StandardForm";
import {BaseOrganizationSettingsView} from "./BaseOrganizationSettingsView";
import {useOrganization} from "../../../hooks/organizations/useOrganization";
import {Button, ListItemText, TextField, Typography} from "@material-ui/core";
import {PermissionSelect} from "../../security/PermissionSelect";
import Box from "@material-ui/core/Box";
import {OrganizationRoleSelect} from "./components/OrganizationRoleSelect";
import {OrganizationRoleType} from "../../../service/response.types";
import {SimpleHateoasList} from "../../common/list/SimpleHateoasList";
import {SimpleListItem} from "../../common/list/SimpleListItem";
import {repositoryPermissionTypes} from "../../../constants/repositoryPermissionTypes";
import ListItemSecondaryAction from "@material-ui/core/ListItemSecondaryAction";
import ListItem from "@material-ui/core/ListItem";

const actions = container.resolve(OrganizationActions);
const messageService = container.resolve(MessageService)

export const OrganizationInvitationsView: FunctionComponent = () => {

    const inviteLoadable = actions.useSelector(state => state.loadables.invite)

    const organization = useOrganization()

    const onCancel = (id: number) => {
        actions.loadableActions.deleteInvitation.dispatch(id);
    };

    return (
        <BaseOrganizationSettingsView title={<T>organization_invitations_title</T>}>
            <StandardForm saveActionLoadable={inviteLoadable}
                          submitButtons={
                              <Button variant="contained" color="primary" type="submit" size="large">
                                  <T>invite_user_generate_invitation_link</T>
                              </Button>}

                          onSubmit={v => actions.loadableActions.invite.dispatch(organization.id, v.type)} initialValues={{type: OrganizationRoleType.MEMBER}}>

                <OrganizationRoleSelect label={<T>invite_user_organization_role_label</T>} name="type" fullWidth/>
            </StandardForm>

            {inviteLoadable.data &&
            <Box mt={2}>
                <TextField fullWidth multiline InputProps={{
                    readOnly: true,
                }} value={LINKS.ACCEPT_INVITATION.buildWithOrigin({[PARAMS.INVITATION_CODE]: inviteLoadable.data.code})}
                           label={<T>invite_user_invitation_code</T>}/>
            </Box>}

            <Box mt={4}>
                <Typography variant="h6"><T>invite_user_active_invitation_codes</T></Typography>
                <Box mt={2}>
                    <SimpleHateoasList actions={actions}
                                       loadableName="listInvitations"
                                       dispatchParams={[organization.id]}
                                       renderItem={(i) => (
                                           <SimpleListItem key={i.id}>
                                               <ListItemText>
                                                   {i.code.substr(0, 10)}...{i.code.substr(i.code.length - 10, 10)}
                                                   &nbsp;[<i><T>invite_user_organization_role_label</T>:
                                                   <T>{`organization_role_type_${OrganizationRoleType[i.type]}`}</T></i>]
                                               </ListItemText>
                                               <ListItemSecondaryAction>
                                                   <Button color="secondary" onClick={() => onCancel(i.id)}><T>invite_user_invitation_cancel_button</T></Button>
                                               </ListItemSecondaryAction>
                                           </SimpleListItem>
                                       )}/>
                </Box>
            </Box>
        </BaseOrganizationSettingsView>
    );
};
