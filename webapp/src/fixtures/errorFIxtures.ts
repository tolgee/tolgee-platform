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


export const parseErrorResponse = (errorData): string[] => {
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
