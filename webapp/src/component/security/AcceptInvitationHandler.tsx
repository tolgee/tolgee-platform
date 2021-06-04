import { default as React, FunctionComponent, useEffect } from 'react';
import { useRouteMatch } from 'react-router-dom';
import { PARAMS } from '../../constants/links';
import { container } from 'tsyringe';
import { FullPageLoading } from '../common/FullPageLoading';
import { ProjectInvitationActions } from '../../store/project/invitations/ProjectInvitationActions';

interface AcceptInvitationHandlerProps {}

const AcceptInvitationHandler: FunctionComponent<AcceptInvitationHandlerProps> =
  (props) => {
    const match = useRouteMatch();

    const code = match.params[PARAMS.INVITATION_CODE];

    const actions = container.resolve(ProjectInvitationActions);

    useEffect(() => actions.acceptInvitation.dispatch(code), []);

    return <FullPageLoading />;
  };
export default AcceptInvitationHandler;
