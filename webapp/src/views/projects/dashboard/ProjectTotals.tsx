import React, { useRef, useState } from 'react';
import clsx from 'clsx';
import { useTranslate } from '@tolgee/react';
import { Box, Menu, MenuItem, styled } from '@mui/material';
import { Edit02 } from '@untitled-ui/icons-react';
import { useHistory } from 'react-router-dom';

import { components } from 'tg.service/apiSchema.generated';
import { useProject } from 'tg.hooks/useProject';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { useConfig, usePreferredOrganization } from 'tg.globalContext/helpers';
import { useCurrentLanguage } from 'tg.hooks/useCurrentLanguage';
import { PercentFormat } from './PercentFormat';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';
import { StringsHint } from 'tg.component/billing/Hints';

const StyledTiles = styled(Box)`
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  grid-template-areas: 'languages text text strings progress progress users tags';
  gap: 10px;

  @container (max-width: 1200px) {
    grid-template-columns: repeat(4, 1fr);
    grid-template-areas:
      'languages text     text     tags'
      'strings   progress progress users';
  }

  @container (max-width: 800px) {
    grid-template-columns: repeat(2, 1fr);
    grid-template-areas:
      'languages tags'
      'text      text'
      'progress  progress'
      'strings   users';
  }
`;

const StyledTile = styled(Box)`
  background-color: ${({ theme }) => theme.palette.tile.background};
  border-radius: 20px;
  height: 120px;
  display: grid;
  gap: 10px;
  grid-auto-flow: column;
  grid-auto-columns: 1fr;
  position: relative;
  text-align: center;

  align-items: stretch;
  color: ${({ theme: { palette } }) => palette.text.primary};

  &.clickable {
    transition: background-color 0.1s ease-out;
    cursor: pointer;

    &:hover {
      background-color: ${({ theme }) => theme.palette.tile.backgroundHover};
      transition: background-color 0.2s ease-in;
    }
  }

  @container (max-width: 1200px) {
    height: 100px;
  }

  @container (max-width: 800px) {
    height: 80px;
  }
`;

const StyledTileDataItem = styled(Box)`
  display: grid;
  grid-template-rows: 1fr auto auto auto 1fr;
  grid-template-areas:
    '.'
    'data'
    'label'
    'sublabel'
    'menu';
  border-radius: 20px;
`;

const StyledTileValue = styled(Box)`
  grid-area: data;
  font-size: 28px;
  display: flex;
  justify-content: center;
  @container (max-width: 800px) {
    font-size: 24px;
  }
`;

const StyledTileDescription = styled('div')`
  grid-area: label;
  padding: 0px 8px;
  font-size: 18px;
  @container (max-width: 800px) {
    font-size: 14px;
  }
`;

const StyledTileEdit = styled(Box)`
  position: absolute;
  top: 0px;
  right: 0px;
  padding: 10px;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

export const ProjectTotals: React.FC<{
  stats: components['schemas']['ProjectStatsModel'];
}> = ({ stats }) => {
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
    history.push(
      LINKS.PROJECT_TRANSLATIONS.build({ [PARAMS.PROJECT_ID]: project.id })
    );
  };

  const redirectToTag = (tag: string) => () => {
    history.push(
      LINKS.PROJECT_TRANSLATIONS.build({ [PARAMS.PROJECT_ID]: project.id }) +
        `?filters=${encodeURIComponent(JSON.stringify({ filterTag: [tag] }))}`
    );
  };

  const redirectToBilling = () => {
    history.push(
      LINKS.ORGANIZATION_BILLING.build({
        [PARAMS.ORGANIZATION_SLUG]: preferredOrganization?.id || '',
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

  const stringsCount = stats.languageStats
    .map((i) => i.reviewedKeyCount + i.translatedKeyCount)
    .reduce((prev, curr) => prev + curr, 0);

  return (
    <>
      <StyledTiles data-cy="project-dashboard-project-totals">
        <StyledTile
          gridArea="languages"
          onClick={canViewTasks ? redirectToTasks : undefined}
          className={clsx({ clickable: canViewTasks })}
          data-cy="project-dashboard-task-count"
        >
          <StyledTileDataItem>
            <StyledTileValue>
              {Number(stats.taskCount).toLocaleString(locale)}
            </StyledTileValue>
            <StyledTileDescription>
              {t('project_dashboard_task_count', {
                count: stats.taskCount,
              })}
            </StyledTileDescription>
          </StyledTileDataItem>
        </StyledTile>

        <StyledTile
          gridArea="text"
          onClick={canViewKeys ? redirectToTranslations : undefined}
          className={clsx({ clickable: canViewKeys })}
          data-cy="project-dashboard-text"
        >
          <StyledTileDataItem data-cy="project-dashboard-key-count">
            <StyledTileValue>
              {Number(stats.keyCount).toLocaleString(locale)}
            </StyledTileValue>
            <StyledTileDescription>
              {t('project_dashboard_key_count', 'Keys', {
                count: stats.keyCount,
              })}
            </StyledTileDescription>
          </StyledTileDataItem>
          <StyledTileDataItem data-cy="project-dashboard-base-word-count">
            <StyledTileValue>
              {Number(stats.baseWordsCount).toLocaleString(locale)}
            </StyledTileValue>
            <StyledTileDescription>
              {t('project_dashboard_base_words_count', 'Base words', {
                count: stats.baseWordsCount,
              })}
            </StyledTileDescription>
          </StyledTileDataItem>
        </StyledTile>

        <StyledTile
          gridArea="progress"
          onClick={canViewKeys ? redirectToTranslations : undefined}
          className={clsx({ clickable: canViewKeys })}
          data-cy="project-dashboard-progress"
        >
          <StyledTileDataItem data-cy="project-dashboard-translated-percentage">
            <StyledTileValue>
              <PercentFormat number={stats.translatedPercentage} />
            </StyledTileValue>
            <StyledTileDescription>
              {t('project_dashboard_translated_percent', 'Translated')}
            </StyledTileDescription>
          </StyledTileDataItem>
          <StyledTileDataItem data-cy="project-dashboard-reviewed-percentage">
            <StyledTileValue>
              <PercentFormat number={stats.reviewedPercentage} />
            </StyledTileValue>
            <StyledTileDescription>
              {t('project_dashboard_reviewed_percent', 'Reviewed')}
            </StyledTileDescription>
          </StyledTileDataItem>
        </StyledTile>

        <StyledTile
          gridArea="strings"
          data-cy="project-dashboard-strings"
          onClick={canGoToBilling ? redirectToBilling : undefined}
          className={clsx({ clickable: canGoToBilling })}
        >
          <StyledTileDataItem data-cy="project-dashboard-strings-count">
            <StyledTileValue>
              {Number(stringsCount).toLocaleString(locale)}
            </StyledTileValue>
            <StyledTileDescription>
              <StringsHint>{t('project_dashboard_strings_count')}</StringsHint>
            </StyledTileDescription>
          </StyledTileDataItem>
        </StyledTile>

        <StyledTile
          gridArea="users"
          data-cy="project-dashboard-members"
          onClick={membersAccessible ? redirectToPermissions : undefined}
          className={clsx({ clickable: membersAccessible })}
        >
          <StyledTileDataItem data-cy="project-dashboard-members-count">
            <StyledTileValue>
              {Number(stats.membersCount).toLocaleString(locale)}
            </StyledTileValue>
            <StyledTileDescription>
              {t('project_dashboard_member_count', 'Members')}
            </StyledTileDescription>
          </StyledTileDataItem>
          {membersEditable && (
            <StyledTileEdit>
              <Edit02 width={20} height={20} />
            </StyledTileEdit>
          )}
        </StyledTile>

        <StyledTile
          gridArea="tags"
          className={clsx({ clickable: tagsClickable })}
          onClick={tagsClickable ? handleMenuOpen : undefined}
          aria-controls={open ? 'basic-menu' : undefined}
          aria-haspopup="true"
          aria-expanded={open ? 'true' : undefined}
          data-cy="project-dashboard-tags"
        >
          <StyledTileDataItem>
            <StyledTileValue>
              {Number(stats.tagCount).toLocaleString(locale)}
            </StyledTileValue>
            <StyledTileDescription>
              {t('project_dashboard_tag_count', 'Tags')}
            </StyledTileDescription>
          </StyledTileDataItem>
        </StyledTile>
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
      </StyledTiles>
    </>
  );
};
