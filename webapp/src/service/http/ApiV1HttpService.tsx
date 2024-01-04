import { ApiHttpService } from './ApiHttpService';

export class ApiV1HttpService extends ApiHttpService {
  apiUrl = import.meta.env.VITE_APP_API_URL + '/api/';
}

export const apiV1HttpService = new ApiV1HttpService();
