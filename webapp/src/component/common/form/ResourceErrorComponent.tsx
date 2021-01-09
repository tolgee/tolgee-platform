import {default as React, FunctionComponent} from "react";
import {Alert} from "../Alert";
import {ErrorResponseDTO} from "../../../service/response.types";
import {T} from "@polygloat/react";
import {Box} from "@material-ui/core";

const standardValidationProp = "STANDARD_VALIDATION";
const customValidationProp = "CUSTOM_VALIDATION";

const isStandardValidationError = (error) => {
    return error.hasOwnProperty(standardValidationProp);
};

const isCustomValidationError = (error) => {
    return error.hasOwnProperty(customValidationProp);
};


const isErrorResponseDTO = (error) => {
    return error.hasOwnProperty("code");
};

export const parseError = (errorData): string[] => {
    if (isStandardValidationError(errorData)) {
        return Object.keys(errorData[standardValidationProp]).map(k => k + "->" + errorData[standardValidationProp][k]);
    }

    if (isCustomValidationError(errorData)) {
        //todo pretty print message with params
        return Object.keys(errorData[customValidationProp]);
    }

    if (isErrorResponseDTO(errorData)) {
        return [errorData.code];
    }

    return errorData && ["Unexpected error"];
};

export const ResourceErrorComponent: FunctionComponent<{ error: ErrorResponseDTO | any }> = (props) => {
    return <>{parseError(props.error).map(e => (
        <Box ml={-2} mr={-2}>
            <Alert key={new Date().toDateString()} severity="error">
                <T>{e}</T>
            </Alert>
        </Box>))}</>;
};