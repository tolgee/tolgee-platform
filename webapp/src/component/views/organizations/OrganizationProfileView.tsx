import * as React from 'react';
import {FunctionComponent, useEffect, useState} from 'react';
import {useSelector} from 'react-redux';
import {container} from 'tsyringe';
import {T, useTranslate} from "@tolgee/react";
import {DashboardPage} from "../../layout/DashboardPage";
import {OrganizationActions} from "../../../store/organization/OrganizationActions";
import {AppState} from "../../../store";
import {LINKS, PARAMS} from "../../../constants/links";
import {Redirect} from "react-router-dom";
import {components} from "../../../service/apiSchema";
import {Validation} from "../../../constants/GlobalValidationSchema";
import {OrganizationFields} from "./OrganizationFields";
import {MessageService} from "../../../service/MessageService";
import {StandardForm} from "../../common/form/StandardForm";
import {BaseOrganizationSettingsView} from "./BaseOrganizationSettingsView";
import {useOrganization} from "../../../hooks/organizations/useOrganization";

const actions = container.resolve(OrganizationActions);
const messageService = container.resolve(MessageService)

export const OrganizationProfileView: FunctionComponent = () => {
    const saveLoadable = useSelector((state: AppState) => state.organizations.loadables.edit);
    const t = useTranslate();

    const organization = useOrganization()

    const onSubmit = (values: components["schemas"]["OrganizationModel"]) => {
        const toSave = {
            name: values.name,
            description: values.description,
            basePermissions: values.basePermissions,
            addressPart: values.addressPart
        } as components["schemas"]["OrganizationDto"];

        actions.loadableActions.edit.dispatch(organization?.id!, toSave);
    };


    const initialValues: components["schemas"]["OrganizationDto"] | null = actions.useSelector(state => state.loadables.get).data
    const [cancelled, setCancelled] = useState(false);

    useEffect(() => {
        if (saveLoadable.loaded) {
            actions.loadableReset.edit.dispatch()
            actions.loadableReset.get.dispatch();
            messageService.success(<T>organization_updated_message</T>)
        }
    }, [saveLoadable.loaded])

    if (cancelled) {
        return <Redirect to={LINKS.ORGANIZATIONS.build()}/>
    }

    if (saveLoadable.loaded) {
        return <Redirect to={LINKS.ORGANIZATION_PROFILE.build({[PARAMS.ORGANIZATION_ADDRESS_PART]: saveLoadable.data?.addressPart!!})}/>
    }

    return (
        <BaseOrganizationSettingsView title={<T>edit_organization_title</T>}>
            <StandardForm initialValues={initialValues!}
                          onSubmit={onSubmit}
                          onCancel={() => setCancelled(true)}
                          saveActionLoadable={saveLoadable}
                          validationSchema={Validation.ORGANIZATION_CREATE_OR_EDIT(t, initialValues?.addressPart)}
            >
                <>
                    <OrganizationFields/>
                </>
            </StandardForm>
        </BaseOrganizationSettingsView>
    );
};
