import { apiV1HttpService } from './http/ApiV1HttpService';

export class SignUpService {
  validateEmail = async (email: string): Promise<boolean> => {
    return apiV1HttpService.post('public/validate_email', email);
  };
}

export const signUpService = new SignUpService();
