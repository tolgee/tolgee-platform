import { useState } from 'react';
import { T } from '@tolgee/react';
import { Button, styled } from '@mui/material';

import { components } from 'tg.service/apiSchema.generated';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { InviteDialog } from 'tg.views/projects/members/component/InviteDialog';

type ContributorModel = components['schemas']['ContributorModel'];

const StyledListItem = styled('div')`
  display: flex;
  border-bottom: 1px solid ${({ theme }) => theme.palette.divider1};
  &:last-child {
    border-bottom: 0;
  }
  position: relative;
  padding: ${({ theme }) => theme.spacing(1)};
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
`;

const StyledItemUser = styled('div')`
  display: flex;
  margin-left: ${({ theme }) => theme.spacing(1)};
  flex-grow: 1;
  align-items: center;
`;

const StyledItemText = styled('div')`
  flex-grow: 1;
  padding: ${({ theme }) => theme.spacing(1)};
`;

const StyledItemDates = styled('div')`
  display: flex;
  gap: ${({ theme }) => theme.spacing(2)};
  align-items: center;
  flex-wrap: wrap;
  color: ${({ theme }) => theme.palette.text.secondary};
  font-size: ${({ theme }) => theme.typography.caption.fontSize};
`;

const StyledItemActions = styled('div')`
  display: flex;
  gap: ${({ theme }) => theme.spacing(1)};
  align-items: center;
  margin-left: ${({ theme }) => theme.spacing(2)};
`;

const StyledPendingLabel = styled('span')`
  color: ${({ theme }) => theme.palette.text.secondary};
  font-size: ${({ theme }) => theme.typography.caption.fontSize};
`;

type Props = {
  contributor: ContributorModel;
};

export const ContributorItem: React.FC<React.PropsWithChildren<Props>> = ({
  contributor,
}) => {
  const formatDate = useDateFormatter();
  const { satisfiesPermission } = useProjectPermissions();
  const canEditMembers = satisfiesPermission('members.edit');
  const [inviteOpen, setInviteOpen] = useState(false);

  const contributorName = contributor.name || `#${contributor.id}`;
  const contributorLabel = contributor.username
    ? `${contributorName} (${contributor.username})`
    : contributorName;

  const formatContribution = (value: string) =>
    formatDate(new Date(value), { dateStyle: 'medium' });
  const firstContribution = formatContribution(contributor.firstContributionAt);
  const lastContribution = formatContribution(contributor.lastContributionAt);

  return (
    <StyledListItem
      data-cy="project-contributor-item"
      data-cy-name={contributorName}
    >
      <StyledItemUser>
        <AvatarImg
          owner={{
            id: contributor.id,
            name: contributor.name,
            avatar: contributor.avatar,
            type: 'USER',
          }}
          size={24}
        />
        <StyledItemText>{contributorLabel}</StyledItemText>
      </StyledItemUser>
      <StyledItemDates>
        <span data-cy="project-contributor-item-first-contribution">
          <T
            keyName="project_contributors_first_contribution"
            defaultValue="First contribution: {date}"
            params={{ date: firstContribution }}
          />
        </span>
        <span data-cy="project-contributor-item-last-contribution">
          <T
            keyName="project_contributors_last_contribution"
            defaultValue="Last contribution: {date}"
            params={{ date: lastContribution }}
          />
        </span>
      </StyledItemDates>
      {canEditMembers && (
        <StyledItemActions>
          {contributor.invitationPending ? (
            <StyledPendingLabel data-cy="project-contributor-invitation-pending">
              <T
                keyName="invite_contributor_pending"
                defaultValue="Invitation pending"
              />
            </StyledPendingLabel>
          ) : (
            <Button
              size="small"
              variant="outlined"
              data-cy="project-contributor-invite-button"
              onClick={() => setInviteOpen(true)}
            >
              <T
                keyName="invite_contributor_button"
                defaultValue="Invite as member"
              />
            </Button>
          )}
        </StyledItemActions>
      )}
      {inviteOpen && (
        <InviteDialog
          open
          onClose={() => setInviteOpen(false)}
          initialEmail={contributor.username}
        />
      )}
    </StyledListItem>
  );
};
