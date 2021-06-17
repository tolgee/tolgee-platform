import { singleton } from 'tsyringe';
import {
  AbstractLoadableActions,
  StateWithLoadables,
} from '../../AbstractLoadableActions';
import { InvitationService } from '../../../service/InvitationService';
import { ApiSchemaHttpService } from '../../../service/http/ApiSchemaHttpService';

export class ProjectInvitationState extends StateWithLoadables<ProjectInvitationActions> {}

@singleton()
export class ProjectInvitationActions extends AbstractLoadableActions<ProjectInvitationState> {
  acceptInvitation = this.createPromiseAction('ACCEPT_INVITATION', (code) =>
    this.invitationService.acceptInvitation(code)
  );

  loadableDefinitions = {
    generateInvitation: this.createLoadableDefinition(
      this.schemaService.schemaRequest('/v2/projects/{projectId}/invite', 'put')
    ),
    list: this.createLoadableDefinition(this.invitationService.getInvitations),
    delete: this.createDeleteDefinition(
      'list',
      async (id) => {
        await this.invitationService.deleteInvitation(id);
        return id;
      },
      (state) => ({ ...state, invitationCode: '' })
    ),
  };

  constructor(
    private invitationService: InvitationService,
    private schemaService: ApiSchemaHttpService
  ) {
    super(new ProjectInvitationState());
  }

  get prefix(): string {
    return 'PROJECT_INVITATION';
  }
}
