import {singleton} from 'tsyringe';
import {remoteConfigService} from '../../service/remoteConfigService';
import {securityService} from '../../service/securityService';
import {userService} from "../../service/userService";
import {AbstractLoadableActions, StateWithLoadables} from "../AbstractLoadableActions";

export class UserState extends StateWithLoadables<UserActions> {
}


@singleton()
export class UserActions extends AbstractLoadableActions<UserState> {
    constructor(private configService: remoteConfigService,
                private securityService: securityService,
                private userService: userService) {
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

