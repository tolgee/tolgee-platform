import {useRouteMatch} from "react-router-dom";
import {container} from "tsyringe";
import {useEffect} from "react";
import {OrganizationActions} from "../../store/organization/OrganizationActions";
import {PARAMS} from "../../constants/links";

const actions = container.resolve(OrganizationActions);

export const useOrganization = () => {
    const match = useRouteMatch();

    const organizationAddressPart = match.params[PARAMS.ORGANIZATION_ADDRESS_PART]
    const resourceLoadable = actions.useSelector(state => state.loadables.get);

    useEffect(() => {
        if (!resourceLoadable.loaded && !resourceLoadable.loading) {
            actions.loadableActions.get.dispatch(organizationAddressPart);
        }
    }, [resourceLoadable.loaded])


    return resourceLoadable.data as NonNullable<typeof resourceLoadable.data>
}
