import { T } from "@tolgee/react";
import React, {useEffect} from "react";
import {container} from "tsyringe";
import {OrganizationActions} from "../../store/organization/OrganizationActions";
import {useSuccessMessage} from "../useSuccessMessage";
import {confirmation} from "../confirmation";

const actions = container.resolve(OrganizationActions);

export const useLeaveOrganization = () => {
    const leaveLoadable = actions.useSelector(state => state.loadables.leave)

    const successMessage = useSuccessMessage()

    useEffect(() => {
        if (leaveLoadable.loaded) {
            actions.loadableReset.leave.dispatch()
            actions.loadableReset.listPermitted.dispatch()
            successMessage(<T>organization_left_message</T>)
        }
    }, [leaveLoadable.loaded])

    return [leaveLoadable, (id) => {
        confirmation({
            message: <T>really_leave_organization_confirmation_message</T>,
            onConfirm: () => actions.loadableActions.leave.dispatch(id)
        })

    }] as [typeof leaveLoadable, (id: number) => void]
}
