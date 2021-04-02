import * as React from 'react';
import {FunctionComponent, useEffect, useState} from 'react';
import {useSelector} from 'react-redux';
import {container} from 'tsyringe';
import {T} from "@tolgee/react";
import {DashboardPage} from "../../layout/DashboardPage";
import {OrganizationActions} from "../../../store/organization/OrganizationActions";
import {AppState} from "../../../store";
import {LINKS} from "../../../constants/links";
import {Redirect} from "react-router-dom";
import {components} from "../../../service/apiSchema";
import {RepositoryPermissionType} from "../../../service/response.types";
import {BaseFormView} from "../../layout/BaseFormView";
import {Validation} from "../../../constants/GlobalValidationSchema";
import {TextField} from "../../common/form/fields/TextField";
import {useFormikContext} from "formik";
import {OrganizationService} from "../../../service/OrganizationService";
import {useDebounce} from "use-debounce";

const actions = container.resolve(OrganizationActions);
const organizationService = container.resolve(OrganizationService);


export const OrganizationCreateView: FunctionComponent = () => {

    const loadable = useSelector((state: AppState) => state.organizations.loadables.create);

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

    const Fields = () => {
        let formik = useFormikContext();
        const [value] = useDebounce(formik.getFieldProps("name").value, 500);

        useEffect(() => {
            const addressPartNotTouchedOrEmpty = !formik.getFieldMeta("addressPart").touched || formik.getFieldProps("addressPart").value === ""
            //autogenerate the addressPart just when not touched and name is valid
            if (formik.getFieldMeta("name").error == undefined && value != "" && addressPartNotTouchedOrEmpty) {
                organizationService.generateAddressPart(value).then((addressPart) => {
                    formik.getFieldHelpers("addressPart").setValue(addressPart)
                    formik.getFieldHelpers("addressPart").setTouched(false)
                })
            }
        }, [value])

        return <>
            <TextField fullWidth label={<T>create_organization_name_label</T>} name="name" required={true}/>
            <TextField fullWidth label={<T>create_organization_addressPart_label</T>} name="addressPart" required={true}/>
            <TextField fullWidth label={<T>create_organization_description_label</T>} name="description"/>
        </>
    }

    return (
        <DashboardPage>
            <BaseFormView lg={6} md={8} title={<T>create_organization_title</T>} initialValues={initialValues} onSubmit={onSubmit}
                          onCancel={() => setCancelled(true)}
                          saveActionLoadable={loadable}
                          validationSchema={Validation.ORGANIZATION_CREATION}
            >
                <>
                    <Fields/>
                </>
            </BaseFormView>
        </DashboardPage>
    );
};
