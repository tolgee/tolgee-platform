import { singleton } from 'tsyringe';
import { RemoteConfigService } from '../../service/RemoteConfigService';
import { SecurityService } from '../../service/SecurityService';
import { UserService } from '../../service/UserService';
import {
  AbstractLoadableActions,
  StateWithLoadables,
} from '../AbstractLoadableActions';

export class UserState extends StateWithLoadables<UserActions> {}

@singleton()
export class UserActions extends AbstractLoadableActions<UserState> {
  constructor(
    private configService: RemoteConfigService,
    private securityService: SecurityService,
    private userService: UserService
  ) {
    super(new UserState());
  }

  readonly loadableDefinitions = {
    userData: this.createLoadableDefinition(this.userService.getUserData),
    updateUser: this.createLoadableDefinition((v) => {
      v.callbackUrl = window.location.protocol + '//' + window.location.host;
      return this.userService.updateUserData(v);
    }),
  };

  get prefix(): string {
    return 'USER';
  }
}
