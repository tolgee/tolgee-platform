import * as React from 'react';
import {FunctionComponent} from 'react';
import {container} from 'tsyringe';
import {T} from "@tolgee/react";
import {OrganizationActions} from "../../../store/organization/OrganizationActions";
import {MessageService} from "../../../service/MessageService";
import {BaseOrganizationSettingsView} from "./BaseOrganizationSettingsView";
import {useOrganization} from "../../../hooks/organizations/useOrganization";
import {OrganizationBasePermissionMenu} from "./components/OrganizationBasePermissionMenu";
import {Box, Typography} from "@material-ui/core";

const actions = container.resolve(OrganizationActions);
const messageService = container.resolve(MessageService)

export const OrganizationMemberPrivilegesView: FunctionComponent = () => {

    const organization = useOrganization()

    return (
        <BaseOrganizationSettingsView title={<T>organization_member_privileges_title</T>}>
            <Typography variant="body1"><T>organization_member_privileges_text</T></Typography>

            <Box mt={2}>
                <OrganizationBasePermissionMenu organization={organization}/>
            </Box>
        </BaseOrganizationSettingsView>
    );
};
