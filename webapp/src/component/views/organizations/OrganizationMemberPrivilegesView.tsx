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
