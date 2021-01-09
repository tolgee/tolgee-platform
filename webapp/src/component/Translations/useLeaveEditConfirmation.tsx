import {confirmation} from "../../hooks/confirmation";
import React from "react";
import {T} from "@polygloat/react";
import {TranslationActions} from "../../store/repository/TranslationActions";
import {container} from "tsyringe";

const actions = container.resolve(TranslationActions);

export const useLeaveEditConfirmationPagination = () => {
    const confirmationData = actions.useSelector(s => {
        if (s.editing?.data?.initialValue !== s.editing?.data?.newValue) {
            return s.editing.data;
        }
        return null;
    });

    return (onConfirm: () => void, onCancel?: () => void) => {
        const cleanOnConfirm = () => {
            onConfirm();
            actions.otherEditionConfirm.dispatch();
        }

        if (confirmationData === null) {
            cleanOnConfirm();
        }
        useLeaveEditConfirmation(confirmationData)(cleanOnConfirm, onCancel);
    };
}

export const useLeaveEditConfirmationOtherEdit = () => {
    const confirmationData = actions.useSelector(s => {
        if (s.editingAfterConfirmation) {
            return s.editing.data;
        }
        return null;
    });

    return useLeaveEditConfirmation(confirmationData);
}

const useLeaveEditConfirmation = (confirmationData) => {
    return (onConfirm: () => void, onCancel?: () => void) => {
        if (confirmationData) {
            confirmation({
                title: <T>translations_leave_save_confirmation</T>,
                message: <T parameters={confirmationData}>translations_leave_save_confirmation_message</T>,
                cancelButtonText: <T>back_to_editing</T>,
                confirmButtonText: <T>discard_changes</T>,
                onConfirm,
                onCancel
            })
        }
    }
}