import React, { ReactNode, useRef, useState } from 'react';
import clsx from 'clsx';
import { useTranslate } from '@tolgee/react';
import { Box, Menu, MenuItem, styled } from '@mui/material';
import { Edit02 } from '@untitled-ui/icons-react';
import { useHistory } from 'react-router-dom';

import { components } from 'tg.service/apiSchema.generated';
import { useProject } from 'tg.hooks/useProject';
import { getProjectTranslationsUrl, LINKS, PARAMS } from 'tg.constants/links';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { useConfig, usePreferredOrganization } from 'tg.globalContext/helpers';
import { useCurrentLanguage } from '@tginternal/library/hooks/useCurrentLanguage';
import { PercentFormat } from './PercentFormat';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';
import {
  KeysHint,
  StringsHint,
  WordsHint,
} from 'tg.component/common/StringsHint';

const StyledGroups = styled(Box)`
  display: grid;
  grid-template-columns: 4fr 2fr 2fr;
  gap: 10px;

  @container (max-width: 1000px) {
    grid-template-columns: 1fr 1fr;

    & > :first-child {
      grid-column: 1 / -1;
    }
  }

  @container (max-width: 600px) {
    grid-template-columns: 1fr;
  }
`;

const StyledGroup = styled(Box)`
  background-color: ${({ theme }) => theme.palette.tile.background};
  border-radius: 20px;
  padding: 12px 10px 10px;
  display: grid;
  gap: 4px;
  align-content: start;
  color: ${({ theme }) => theme.palette.text.primary};
`;

const StyledGroupTitle = styled('div')`
  padding: 0px 10px;
  text-align: center;
  font-size: 11px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

const StyledRow = styled(Box)`
  display: grid;
  gap: 10px;
  grid-auto-flow: column;
  grid-auto-columns: 1fr;
`;

const StyledContentRow = styled(StyledRow)`
  @container (max-width: 600px) {
    grid-auto-flow: row;
    grid-template-columns: 1fr 1fr;
  }
`;

const StyledNumber = styled(Box)`
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-width: 0;
  padding: 8px 0px;
  border-radius: 14px;
  text-align: center;

  &.clickable {
    transition: background-color 0.1s ease-out;
    cursor: pointer;

    &:hover {
      background-color: ${({ theme }) => theme.palette.tile.backgroundHover};
      transition: background-color 0.2s ease-in;
    }
  }
`;

const StyledTileValue = styled(Box)`
  font-size: 28px;
  @container (max-width: 800px) {
    font-size: 24px;
  }
`;

const StyledTileDescription = styled('div')`
  max-width: 100%;
  padding: 0px 8px;
  font-size: 18px;
  @container (max-width: 800px) {
    font-size: 14px;
  }
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`;

const StyledTileEdit = styled(Box)`
  position: absolute;
  top: 0px;
  right: 0px;
  padding: 10px;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

type StatProps = {
  dataCy: string;
  value: ReactNode;
  label: ReactNode;
} & Omit<React.ComponentProps<typeof StyledNumber>, 'label'>;

const Stat = ({
  dataCy,
  value,
  label,
  onClick,
  children,
  ...rest
}: StatProps) => (
  <StyledNumber
    {...rest}
    onClick={onClick}
    className={clsx({ clickable: Boolean(onClick) })}
    data-cy={dataCy}
  >
    <StyledTileValue>{value}</StyledTileValue>
    <StyledTileDescription>{label}</StyledTileDescription>
    {children}
  </StyledNumber>
);

export const ProjectTotals: React.FC<
  React.PropsWithChildren<{
    stats: components['schemas']['ProjectStatsModel'];
  }>
> = ({ stats }) => {
  const { t } = useTranslate();
  const project = useProject();
  const history = useHistory();
  const config = useConfig();
  const tags = useApiQuery({
    url: '/v2/projects/{projectId}/tags',
    method: 'get',
    path: { projectId: project.id },
    query: { size: 1000 },
  });
  const locale = useCurrentLanguage();

  const billingEnabled = useGlobalContext(
    (c) => c.initialData.serverConfiguration.billing.enabled
  );
  const isOrganizationOwner = useGlobalContext(
    (c) => c.initialData.preferredOrganization?.currentUserRole === 'OWNER'
  );
  const { preferredOrganization } = usePreferredOrganization();
  const canGoToBilling = billingEnabled && isOrganizationOwner;

  const [anchorEl, setAnchorEl] = useState<HTMLDivElement | null>(null);
  const anchorWidth = useRef();
  const open = Boolean(anchorEl);
  const handleMenuOpen = (event) => {
    anchorWidth.current = event.currentTarget.offsetWidth;
    setAnchorEl(event.currentTarget);
  };
  const handleMenuClose = () => {
    setAnchorEl(null);
  };

  const redirectToTasks = () => {
    history.push(
      LINKS.PROJECT_TASKS.build({ [PARAMS.PROJECT_ID]: project.id })
    );
  };

  const redirectToPermissions = () => {
    history.push(
      LINKS.PROJECT_PERMISSIONS.build({ [PARAMS.PROJECT_ID]: project.id })
    );
  };

  const redirectToTranslations = () => {
    history.push(getProjectTranslationsUrl(project.id));
  };

  const redirectToTag = (tag: string) => () => {
    history.push(
      getProjectTranslationsUrl(project.id, {
        filters: { filterTag: [tag] },
      })
    );
  };

  const redirectToBilling = () => {
    history.push(
      LINKS.ORGANIZATION_BILLING.build({
        [PARAMS.ORGANIZATION_SLUG]: preferredOrganization?.slug || '',
      })
    );
  };

  const { satisfiesPermission } = useProjectPermissions();

  const canViewMembers = satisfiesPermission('members.view');
  const canViewKeys = satisfiesPermission('keys.view');
  const canEditMembers = satisfiesPermission('members.edit');
  const canViewTasks = satisfiesPermission('tasks.view');

  const tagsPresent = Boolean(stats.tagCount);
  const tagsClickable = tagsPresent && canViewKeys;

  const membersAccessible = config.authentication && canViewMembers;
  const membersEditable = membersAccessible && canEditMembers;

  const stringsCount = stats.languageStats.reduce(
    (sum, i) => sum + i.reviewedKeyCount + i.translatedKeyCount,
    0
  );

  return (
    <StyledGroups data-cy="project-dashboard-project-totals">
      <StyledGroup data-cy="project-dashboard-content">
        <StyledGroupTitle>
          {t('project_dashboard_group_content', 'Content')}
        </StyledGroupTitle>
        <StyledContentRow>
          <Stat
            dataCy="project-dashboard-key-count"
            onClick={canViewKeys ? redirectToTranslations : undefined}
            value={Number(stats.keyCount).toLocaleString(locale)}
            label={
              <KeysHint>
                {t('project_dashboard_key_count_plural', 'Keys', {
                  value: stats.keyCount,
                })}
              </KeysHint>
            }
          />
          <Stat
            dataCy="project-dashboard-strings-count"
            onClick={canGoToBilling ? redirectToBilling : undefined}
            value={Number(stringsCount).toLocaleString(locale)}
            label={
              <StringsHint>
                {t('project_dashboard_strings_count_plural', {
                  value: Number(stringsCount),
                })}
              </StringsHint>
            }
          />
          <Stat
            dataCy="project-dashboard-words-count"
            onClick={canGoToBilling ? redirectToBilling : undefined}
            value={Number(stats.hostedWordsCount).toLocaleString(locale)}
            label={
              <WordsHint>
                {t(
                  'project_dashboard_words_count_plural',
                  '{value, plural, one {Word} other {Words}}',
                  { value: Number(stats.hostedWordsCount) }
                )}
              </WordsHint>
            }
          />
          <Stat
            dataCy="project-dashboard-tags"
            onClick={tagsClickable ? handleMenuOpen : undefined}
            aria-controls={open ? 'basic-menu' : undefined}
            aria-haspopup="true"
            aria-expanded={open ? 'true' : undefined}
            value={Number(stats.tagCount).toLocaleString(locale)}
            label={t('project_dashboard_tag_count_plural', {
              value: Number(stats.tagCount),
            })}
          />
        </StyledContentRow>
      </StyledGroup>

      <StyledGroup data-cy="project-dashboard-progress">
        <StyledGroupTitle>
          {t('project_dashboard_group_progress', 'Progress')}
        </StyledGroupTitle>
        <StyledRow>
          <Stat
            dataCy="project-dashboard-translated-percentage"
            onClick={canViewKeys ? redirectToTranslations : undefined}
            value={<PercentFormat number={stats.translatedPercentage} />}
            label={t('project_dashboard_translated_percent')}
          />
          <Stat
            dataCy="project-dashboard-reviewed-percentage"
            onClick={canViewKeys ? redirectToTranslations : undefined}
            value={<PercentFormat number={stats.reviewedPercentage} />}
            label={t('project_dashboard_reviewed_percent', 'Reviewed')}
          />
        </StyledRow>
      </StyledGroup>

      <StyledGroup data-cy="project-dashboard-team">
        <StyledGroupTitle>
          {t('project_dashboard_group_team', 'Team')}
        </StyledGroupTitle>
        <StyledRow>
          <Stat
            dataCy="project-dashboard-task-count"
            onClick={canViewTasks ? redirectToTasks : undefined}
            value={Number(stats.taskCount).toLocaleString(locale)}
            label={t('project_dashboard_task_count_plural', {
              value: stats.taskCount,
            })}
          />
          <Stat
            dataCy="project-dashboard-members-count"
            onClick={membersAccessible ? redirectToPermissions : undefined}
            value={
              canViewMembers
                ? Number(stats.membersCount).toLocaleString(locale)
                : '—'
            }
            label={
              canViewMembers
                ? t('project_dashboard_member_count_plural', {
                    value: Number(stats.membersCount),
                  })
                : t('project_menu_members')
            }
          >
            {membersEditable && (
              <StyledTileEdit>
                <Edit02 width={20} height={20} />
              </StyledTileEdit>
            )}
          </Stat>
        </StyledRow>
      </StyledGroup>

      <Menu
        PaperProps={{ sx: { minWidth: anchorWidth.current } }}
        anchorEl={anchorEl}
        open={open}
        onClose={handleMenuClose}
        anchorOrigin={{
          vertical: 'bottom',
          horizontal: 'center',
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'center',
        }}
      >
        {tags.data?._embedded?.tags?.map((tag) => (
          <MenuItem key={tag.id} onClick={redirectToTag(tag.name)}>
            {tag.name}
          </MenuItem>
        ))}
      </Menu>
    </StyledGroups>
  );
};
