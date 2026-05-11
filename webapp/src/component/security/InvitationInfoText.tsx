import { T } from '@tolgee/react';
import { components } from 'tg.service/apiSchema.generated';

type InvitationData = components['schemas']['PublicInvitationModel'];

export function InvitationInfoText({ data }: { data: InvitationData }) {
  const username = data.createdBy?.name ?? data.createdBy?.username;
  const project = data.projectName;
  const organization = data.organizationName;
  const params = { username, project, organization, b: <b /> };

  if (project) {
    if (username) {
      return (
        <T
          keyName="accept_invitation_description_project_user"
          params={params}
        />
      );
    }
    return (
      <T keyName="accept_invitation_description_project" params={params} />
    );
  }
  if (data.createdBy) {
    return (
      <T
        keyName="accept_invitation_description_organization_user"
        params={params}
      />
    );
  }
  return (
    <T keyName="accept_invitation_description_organization" params={params} />
  );
}
