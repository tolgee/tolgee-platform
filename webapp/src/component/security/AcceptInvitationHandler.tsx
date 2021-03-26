import {default as React, FunctionComponent, useEffect} from 'react';
import {useRouteMatch} from 'react-router-dom';
import {PARAMS} from '../../constants/links';
import {container} from 'tsyringe';
import {RepositoryInvitationActions} from '../../store/repository/invitations/RepositoryInvitationActions';
import {SecurityService} from "../../service/SecurityService";
import {FullPageLoading} from "../common/FullPageLoading";

interface AcceptInvitationHandlerProps {

}

const securityServiceIns = container.resolve(SecurityService);

const AcceptInvitationHandler: FunctionComponent<AcceptInvitationHandlerProps> = (props) => {
    const match = useRouteMatch();

    const code = match.params[PARAMS.INVITATION_CODE];

    const actions = container.resolve(RepositoryInvitationActions);

    useEffect(() => actions.acceptInvitation.dispatch(code), []);

    return <FullPageLoading/>;
};
export default AcceptInvitationHandler;