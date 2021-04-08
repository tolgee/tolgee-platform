import * as React from 'react';
import {FunctionComponent, useEffect, useState} from 'react';
import {useSelector} from 'react-redux';
import {container} from 'tsyringe';
import {T, useTranslate} from "@tolgee/react";
import {DashboardPage} from "../../layout/DashboardPage";
import {OrganizationActions} from "../../../store/organization/OrganizationActions";
import {AppState} from "../../../store";
import {LINKS} from "../../../constants/links";
import {Redirect} from "react-router-dom";
import {components} from "../../../service/apiSchema";
import {RepositoryPermissionType} from "../../../service/response.types";
import {BaseFormView} from "../../layout/BaseFormView";
import {Validation} from "../../../constants/GlobalValidationSchema";
import {OrganizationFields} from "./components/OrganizationFields";

const actions = container.resolve(OrganizationActions);


export const OrganizationCreateView: FunctionComponent = () => {

    const loadable = useSelector((state: AppState) => state.organizations.loadables.create);
    const t = useTranslate();

    const onSubmit = (values) => {
        actions.loadableActions.create.dispatch(values);
    };

    const initialValues: components["schemas"]["OrganizationDto"] = {
        name: '',
        addressPart: '',
        description: '',
        basePermissions: RepositoryPermissionType.VIEW
    };

    const [cancelled, setCancelled] = useState(false);

    useEffect(() => {
        return () => actions.loadableReset.create.dispatch()
    }, [])

    if (cancelled) {
        return <Redirect to={LINKS.ORGANIZATIONS.build()}/>
    }

    if (loadable.loaded) {
        return <Redirect to={LINKS.ORGANIZATIONS.build()}/>
    }

    return (
        <DashboardPage>
            <BaseFormView lg={6} md={8} title={<T>create_organization_title</T>} initialValues={initialValues} onSubmit={onSubmit}
                          onCancel={() => setCancelled(true)}
                          saveActionLoadable={loadable}
                          validationSchema={Validation.ORGANIZATION_CREATE_OR_EDIT(t, '')}
            >
                <>
                    <OrganizationFields/>
                </>
            </BaseFormView>
        </DashboardPage>
    );
};
