import { T, useTranslate } from '@tolgee/react';
import { styled } from '@mui/material';

import { components } from 'tg.service/apiSchema.generated';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { useDateFormatter } from 'tg.hooks/useLocale';

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

type Props = {
  contributor: ContributorModel;
};

export const ContributorItem: React.FC<React.PropsWithChildren<Props>> = ({
  contributor,
}) => {
  const { t } = useTranslate();
  const formatDate = useDateFormatter();
  const formatContribution = (value: string) =>
    formatDate(new Date(value), { dateStyle: 'medium' });
  const firstContribution = formatContribution(contributor.firstContributionAt);
  const lastContribution = formatContribution(contributor.lastContributionAt);

  return (
    <StyledListItem
      data-cy="project-contributor-item"
      data-cy-name={contributor.name}
    >
      <StyledItemUser>
        <AvatarImg owner={{ ...contributor, type: 'USER' }} size={24} />
        <StyledItemText>
          {contributor.name ||
            t('project_contributor_unnamed', 'Unnamed contributor')}
        </StyledItemText>
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
    </StyledListItem>
  );
};
