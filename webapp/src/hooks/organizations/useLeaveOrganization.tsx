import {T} from "@tolgee/react";
import React, {ReactNode} from "react";
import {container} from "tsyringe";
import {OrganizationActions} from "../../store/organization/OrganizationActions";
import {confirmation} from "../confirmation";
import {ResourceErrorComponent} from "../../component/common/form/ResourceErrorComponent";
import {Box} from "@material-ui/core";

const actions = container.resolve(OrganizationActions);

export const useLeaveOrganization = () => {
    const leaveLoadable = actions.useSelector(state => state.loadables.leave)

    return [(id) => {
        confirmation({
            message: <T>really_leave_organization_confirmation_message</T>,
            onConfirm: () => actions.loadableActions.leave.dispatch(id)
        })

    }, <Box ml={2} mr={2}><ResourceErrorComponent error={leaveLoadable.error}/></Box>] as [(id: number) => void, ReactNode]
}
