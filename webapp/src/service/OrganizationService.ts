import { singleton } from 'tsyringe';

import { ApiV2HttpService } from './http/ApiV2HttpService';

@singleton()
export class OrganizationService {
  constructor(private v2http: ApiV2HttpService) {}

  public generateSlug = (name: string, oldSlug?: string) =>
    this.v2http.post(`slug/generate-organization`, {
      name: name,
      oldSlug: oldSlug,
    }) as Promise<string>;

  public validateSlug = (slug: string) =>
    this.v2http.get(`slug/validate-organization/${slug}`) as Promise<boolean>;
}
