import {singleton} from 'tsyringe';
import {RemoteConfigService} from '../../service/remoteConfigService';
import {SecurityService} from '../../service/SecurityService';
import {UserService} from "../../service/userService";
import {AbstractLoadableActions, StateWithLoadables} from "../AbstractLoadableActions";

export class UserState extends StateWithLoadables<UserActions> {
}


@singleton()
export class UserActions extends AbstractLoadableActions<UserState> {
    constructor(private configService: RemoteConfigService,
                private securityService: SecurityService,
                private userService: UserService) {
        super(new UserState());
    }

    readonly loadableDefinitions = {
        userData: this.createLoadableDefinition(this.userService.getUserData),
        updateUser: this.createLoadableDefinition(this.userService.updateUserData),
    };


    get prefix(): string {
        return 'USER';
    }
}

