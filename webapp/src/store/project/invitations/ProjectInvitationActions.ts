import { singleton } from 'tsyringe';
import {
  AbstractLoadableActions,
  StateWithLoadables,
} from '../../AbstractLoadableActions';
import { InvitationService } from '../../../service/InvitationService';

export class ProjectInvitationState extends StateWithLoadables<ProjectInvitationActions> {
  invitationLoading = false;
  invitationCode: string | null = null;
}

@singleton()
export class ProjectInvitationActions extends AbstractLoadableActions<ProjectInvitationState> {
  generateCode = this.createPromiseAction('GENERATE_CODE', (projectId, type) =>
    this.invitationService.generateInvitationCode(projectId, type)
  )
    .build.onPending((state) => {
      return { ...state, invitationCode: null, invitationLoading: true };
    })
    .build.onFullFilled((state, action) => {
      return {
        ...state,
        invitationCode: action.payload,
        invitationLoading: false,
      };
    })
    .build.onRejected((state) => {
      return { ...state, invitationCode: null, invitationLoading: false };
    });

  acceptInvitation = this.createPromiseAction('ACCEPT_INVITATION', (code) =>
    this.invitationService.acceptInvitation(code)
  );

  loadableDefinitions = {
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

  constructor(private invitationService: InvitationService) {
    super(new ProjectInvitationState());
  }

  get prefix(): string {
    return 'REPOSITORY_INVITATION';
  }
}
