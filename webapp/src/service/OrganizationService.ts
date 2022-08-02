import { apiV2HttpService } from './http/ApiV2HttpService';

export class OrganizationService {
  public generateSlug = (name: string, oldSlug?: string) =>
    apiV2HttpService.post(`slug/generate-organization`, {
      name: name,
      oldSlug: oldSlug,
    }) as Promise<string>;

  public validateSlug = (slug: string) =>
    apiV2HttpService.get(
      `slug/validate-organization/${slug}`
    ) as Promise<boolean>;
}

export const organizationService = new OrganizationService();
