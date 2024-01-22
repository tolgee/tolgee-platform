import { ApiV1HttpService } from './ApiV1HttpService';

export class ApiV2HttpService extends ApiV1HttpService {
  apiUrl = import.meta.env.VITE_APP_API_URL + '/v2/';
}

export const apiV2HttpService = new ApiV2HttpService();
