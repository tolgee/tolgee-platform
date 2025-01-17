import { ApiError } from 'tg.service/http/ApiError';

export const parseErrorResponse = (errorData: ApiError): string[] => {
  if (errorData.STANDARD_VALIDATION) {
    return Object.keys(errorData.STANDARD_VALIDATION).map(
      (k) => k + '->' + errorData.STANDARD_VALIDATION?.[k]
    );
  }

  if (errorData.CUSTOM_VALIDATION) {
    return Object.keys(errorData.CUSTOM_VALIDATION);
  }

  if (errorData.code) {
    return [errorData.code];
  }

  return ['unexpected_error_occurred'];
};
