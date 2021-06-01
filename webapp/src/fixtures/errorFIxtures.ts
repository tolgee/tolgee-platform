const standardValidationProp = 'STANDARD_VALIDATION';
const customValidationProp = 'CUSTOM_VALIDATION';

const isStandardValidationError = (error) => {
  // eslint-disable-next-line no-prototype-builtins
  return error.hasOwnProperty(standardValidationProp);
};

const isCustomValidationError = (error) => {
  // eslint-disable-next-line no-prototype-builtins
  return error.hasOwnProperty(customValidationProp);
};

const isErrorResponseDTO = (error) => {
  // eslint-disable-next-line no-prototype-builtins
  return error.hasOwnProperty('code');
};

export const parseErrorResponse = (errorData): string[] => {
  if (isStandardValidationError(errorData)) {
    return Object.keys(errorData[standardValidationProp]).map(
      (k) => k + '->' + errorData[standardValidationProp][k]
    );
  }

  if (isCustomValidationError(errorData)) {
    //todo pretty print message with params
    return Object.keys(errorData[customValidationProp]);
  }

  if (isErrorResponseDTO(errorData)) {
    return [errorData.code];
  }

  return errorData && ['Unexpected error'];
};
