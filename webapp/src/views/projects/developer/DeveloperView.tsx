import { Box, styled, Tab, Tabs } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { Link, useRouteMatch } from 'react-router-dom';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { BaseProjectView } from '../BaseProjectView';
import { CdList } from './contentDelivery/CdList';
import { StorageList } from './storage/StorageList';
import { WebhookList } from './webhook/WebhookList';

const StyledTabs = styled(Tabs)`
  margin-bottom: -1px;
`;

const StyledTabWrapper = styled(Box)`
  border-bottom: 1px solid ${({ theme }) => theme.palette.divider1};
`;

export const DeveloperView = () => {
  const project = useProject();
  const { t } = useTranslate();
  const { satisfiesPermission } = useProjectPermissions();
  const canPublishCd = satisfiesPermission('content-delivery.publish');
  const canManageCd = satisfiesPermission('content-delivery.manage');
  const canManageWebhooks = satisfiesPermission('webhooks.manage');

  const pageCd = useRouteMatch(LINKS.PROJECT_CONTENT_STORAGE.template);
  const pageStorage = useRouteMatch(LINKS.PROJECT_STORAGE.template);
  const pageWebhooks = useRouteMatch(LINKS.PROJECT_WEBHOOKS.template);

  return (
    <BaseProjectView
      windowTitle={t('automation_view_title')}
      title={t('automation_view_title')}
      maxWidth="normal"
      navigation={[
        [
          t('automation_view_title'),
          LINKS.PROJECT_TRANSLATIONS.build({
            [PARAMS.PROJECT_ID]: project.id,
          }),
        ],
      ]}
    >
      <StyledTabWrapper>
        <StyledTabs
          value={
            pageCd?.isExact
              ? 'content-delivery'
              : pageStorage?.isExact
              ? 'storage'
              : pageWebhooks?.isExact
              ? 'webhooks'
              : null
          }
        >
          {canPublishCd && (
            <Tab
              value="content-delivery"
              component={Link}
              to={LINKS.PROJECT_CONTENT_STORAGE.build({
                [PARAMS.PROJECT_ID]: project.id,
              })}
              label={t('developer_menu_content_delivery')}
              data-cy="developer-menu-content-delivery"
            />
          )}
          {canManageCd && (
            <Tab
              value="storage"
              component={Link}
              to={LINKS.PROJECT_STORAGE.build({
                [PARAMS.PROJECT_ID]: project.id,
              })}
              label={t('developer_menu_storage')}
              data-cy="developer-menu-storage"
            />
          )}
          {canManageWebhooks && (
            <Tab
              value="webhooks"
              component={Link}
              to={LINKS.PROJECT_WEBHOOKS.build({
                [PARAMS.PROJECT_ID]: project.id,
              })}
              label={t('developer_menu_webhooks')}
              data-cy="developer-menu-webhooks"
            />
          )}
        </StyledTabs>
      </StyledTabWrapper>

      {pageCd?.isExact ? (
        <CdList />
      ) : pageStorage ? (
        <StorageList />
      ) : pageWebhooks ? (
        <WebhookList />
      ) : null}
    </BaseProjectView>
  );
};
