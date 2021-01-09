import {singleton} from 'tsyringe';
import {AbstractLoadableActions, StateWithLoadables} from "../../AbstractLoadableActions";
import {invitationService} from "../../../service/invitationService";

export class RepositoryInvitationState extends StateWithLoadables<RepositoryInvitationActions> {
    invitationLoading: boolean = false;
    invitationCode: string = null;
}

@singleton()
export class RepositoryInvitationActions extends AbstractLoadableActions<RepositoryInvitationState> {

    generateCode = this.createPromiseAction('GENERATE_CODE',
        (repositoryId, type) => this.invitationService.generateInvitationCode(repositoryId, type))
        .build.onPending((state, action) => {
            return {...state, invitationCode: null, invitationLoading: true};
        }).build.onFullFilled((state, action) => {
            return {...state, invitationCode: action.payload, invitationLoading: false};
        }).build.onRejected((state, action) => {
            return {...state, invitationCode: null, invitationLoading: false};
        });

    acceptInvitation = this.createPromiseAction('ACCEPT_INVITATION',
        (code) => this.invitationService.acceptInvitation(code));


    loadableDefinitions = {
        list: this.createLoadableDefinition(this.invitationService.getInvitations),
        delete: this.createDeleteDefinition("list", async (id) => {
            await this.invitationService.deleteInvitation(id);
            return id;
        }, (state) => ({...state, invitationCode: ""}))
    };

    constructor(private invitationService: invitationService) {
        super(new RepositoryInvitationState());
    }

    get prefix(): string {
        return 'REPOSITORY_INVITATION';
    }
}

