import { useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Button, styled } from '@mui/material';

import { components } from 'tg.service/apiSchema.generated';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { useProject } from 'tg.hooks/useProject';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { useProjectLanguages } from 'tg.hooks/useProjectLanguages';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { PermissionsModal } from 'tg.component/PermissionsSettings/PermissionsModal';
import {
  PermissionModel,
  PermissionSettingsState,
} from 'tg.component/PermissionsSettings/types';
import { useCreateContributorInvitation } from './useCreateContributorInvitation';

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

const INITIAL_PERMISSIONS: PermissionModel = {
  type: 'TRANSLATE',
  scopes: [],
};

type Props = {
  contributor: ContributorModel;
};

export const ContributorItem: React.FC<React.PropsWithChildren<Props>> = ({
  contributor,
}) => {
  const { t } = useTranslate();
  const formatDate = useDateFormatter();
  const project = useProject();
  const allLangs = useProjectLanguages();
  const messages = useMessage();
  const { satisfiesPermission } = useProjectPermissions();
  const canEditMembers = satisfiesPermission('members.edit');

  const [inviteOpen, setInviteOpen] = useState(false);
  const { createContributorInvitation } = useCreateContributorInvitation({
    projectId: project.id,
  });

  const contributorName =
    contributor.name || t('project_contributor_unnamed', 'Unnamed contributor');

  const formatContribution = (value: string) =>
    formatDate(new Date(value), { dateStyle: 'medium' });
  const firstContribution = formatContribution(contributor.firstContributionAt);
  const lastContribution = formatContribution(contributor.lastContributionAt);

  async function handleInvite(permissions: PermissionSettingsState) {
    await createContributorInvitation({
      userId: contributor.id,
      permissions,
    });
    messages.success(
      <T
        keyName="invite_contributor_success_message"
        defaultValue="Contributor invited as a member"
      />
    );
  }

  return (
    <StyledListItem
      data-cy="project-contributor-item"
      data-cy-name={contributor.name}
    >
      <StyledItemUser>
        <AvatarImg owner={{ ...contributor, type: 'USER' }} size={24} />
        <StyledItemText>{contributorName}</StyledItemText>
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
        </StyledItemActions>
      )}
      {inviteOpen && (
        <PermissionsModal
          allLangs={allLangs}
          title={contributorName}
          permissions={INITIAL_PERMISSIONS}
          onSubmit={handleInvite}
          onClose={() => setInviteOpen(false)}
          hideNone
        />
      )}
    </StyledListItem>
  );
};
