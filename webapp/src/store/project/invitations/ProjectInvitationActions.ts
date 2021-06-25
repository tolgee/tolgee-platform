import { singleton } from 'tsyringe';

import {
  AbstractLoadableActions,
  StateWithLoadables,
} from '../../AbstractLoadableActions';

export class ProjectInvitationState extends StateWithLoadables<ProjectInvitationActions> {
  invitationCode: string | null = null;
}

@singleton()
export class ProjectInvitationActions extends AbstractLoadableActions<ProjectInvitationState> {
  constructor() {
    super(new ProjectInvitationState());
  }

  setCode = this.createAction('SET_CODE', (code: string) => code).build.on(
    (state, action) => {
      return {
        ...state,
        invitationCode: action.payload,
      };
    }
  );

  loadableDefinitions = {};

  get prefix(): string {
    return 'PROJECT_INVITATION';
  }
}
