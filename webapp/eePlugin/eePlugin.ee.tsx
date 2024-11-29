import { EePluginType } from './plugin/EePluginType';
import { PermissionsAdvancedEe } from '../src/ee/PermissionsAdvanced/PermissionsAdvancedEe';
import { BillingMenuItem } from '../src/ee/billing/component/UserMenu/BillingMenuItem';
import { PrivateRoute } from '../src/component/common/PrivateRoute';
import { LINKS } from '../src/constants/links';
import React from 'react';
import { MyTasksView } from '../src/ee/task/views/myTasks/MyTasksView';
import { TaskReference } from '../src/ee/task/components/TaskReference';
import { useGlobalContext } from '../src/globalContext/GlobalContext';
import { useUserTasks } from '../src/globalContext/useUserTasks';
import { AdministrationCloudPlansView } from '../src/ee/billing/administration/AdministrationCloudPlansView';
import { AdministrationCloudPlanCreateView } from '../src/ee/billing/administration/AdministrationCloudPlanCreateView';
import { AdministrationCloudPlanEditView } from '../src/ee/billing/administration/AdministrationCloudPlanEditView';
import { AdministrationEePlansView } from '../src/ee/billing/administration/AdministrationEePlansView';
import { AdministrationEePlanCreateView } from '../src/ee/billing/administration/AdministrationEePlanCreateView';
import { AdministrationEePlanEditView } from '../src/ee/billing/administration/AdministrationEePlanEditView';
import { AdministrationEeLicenseView } from '../src/ee/billing/administration/AdministrationEeLicenseView';
import { SlackApp } from '../src/ee/organizationApps/SlackApp';
import { Usage } from '../src/ee/billing/component/Usage';
import { useConfig, useEnabledFeatures } from '../src/globalContext/helpers';
import { OrganizationSubscriptionsView } from '../src/ee/billing/Subscriptions/OrganizationSubscriptionsView';
import { OrganizationInvoicesView } from '../src/ee/billing/Invoices/OrganizationInvoicesView';
import { OrganizationBillingView } from '../src/ee/billing/OrganizationBillingView';
import { OrganizationBillingTestClockHelperView } from '../src/ee/billing/OrganizationBillingTestClockHelperView';
import { Link, Route, Switch } from 'react-router-dom';
import { ProjectTasksView } from '../src/ee/task/views/projectTasks/ProjectTasksView';
import { addOperations } from '../src/views/projects/translations/BatchOperations/operations';
import { OperationTaskCreate } from '../src/ee/batchOperations/OperationTaskCreate';
import { OperationTaskAddKeys } from '../src/ee/batchOperations/OperationTaskAddKeys';
import { OperationTaskRemoveKeys } from '../src/ee/batchOperations/OperationTaskRemoveKeys';
import { useTranslationsSelector } from '../src/views/projects/translations/context/TranslationsContext';
import { useProjectPermissions } from '../src/hooks/useProjectPermissions';
import { T, useTranslate } from '@tolgee/react';
import { TranslationTaskIndicator } from '../src/ee/task/components/TranslationTaskIndicator';
import { PrefilterTask } from '../src/ee/task/components/PrefilterTask';
import { addPanel } from '../src/views/projects/translations/ToolsPanel/panelsList';
import { ClipboardCheck } from '@untitled-ui/icons-react';
import { tasksCount, TasksPanel } from '../src/ee/task/components/TasksPanel';
import { TranslationsTaskDetail } from '../src/ee/task/components/TranslationsTaskDetail';
import { GlobalLimitPopover } from '../src/ee/billing/limitPopover/GlobalLimitPopover';
import { addDeveloperViewItems } from '../src/views/projects/developer/developerViewItems';
import { StorageList } from '../src/ee/developer/storage/StorageList';
import { WebhookList } from '../src/ee/developer/webhook/WebhookList';
import { Badge, Box, MenuItem } from '@mui/material';
import { addUserMenuItems } from '../src/component/security/UserMenu/UserMenuItems';
import { addProjectMenuItems } from '../src/views/projects/projectMenu/ProjectMenu';

export const eePlugin: EePluginType = {
  ee: {
    activity: {
      TaskReference: TaskReference,
    },
    PermissionsAdvanced: PermissionsAdvancedEe,
    billing: {
      billingMenuItems: [BillingMenuItem],
      GlobalLimitPopover,
    },
    organization: {
      apps: SlackApp,
      Usage,
    },
    routes: {
      Root: () => {
        return (
          <Switch>
            <PrivateRoute exact path={LINKS.MY_TASKS.template}>
              <MyTasksView />
            </PrivateRoute>
          </Switch>
        );
      },
      Administration: () => (
        <Switch>
          <PrivateRoute exact path={LINKS.ADMINISTRATION_EE_LICENSE.template}>
            <AdministrationEeLicenseView />
          </PrivateRoute>
          <PrivateRoute
            exact
            path={LINKS.ADMINISTRATION_BILLING_CLOUD_PLANS.template}
          >
            <AdministrationCloudPlansView />
          </PrivateRoute>
          <PrivateRoute
            exact
            path={LINKS.ADMINISTRATION_BILLING_CLOUD_PLAN_CREATE.template}
          >
            <AdministrationCloudPlanCreateView />
          </PrivateRoute>
          <PrivateRoute
            exact
            path={LINKS.ADMINISTRATION_BILLING_CLOUD_PLAN_EDIT.template}
          >
            <AdministrationCloudPlanEditView />
          </PrivateRoute>
          <PrivateRoute
            exact
            path={LINKS.ADMINISTRATION_BILLING_EE_PLANS.template}
          >
            <AdministrationEePlansView />
          </PrivateRoute>
          <PrivateRoute
            exact
            path={LINKS.ADMINISTRATION_BILLING_EE_PLAN_CREATE.template}
          >
            <AdministrationEePlanCreateView />
          </PrivateRoute>
          <PrivateRoute
            exact
            path={LINKS.ADMINISTRATION_BILLING_EE_PLAN_EDIT.template}
          >
            <AdministrationEePlanEditView />
          </PrivateRoute>
        </Switch>
      ),
      Organization: () => {
        const config = useConfig();
        return (
          <>
            {config.billing.enabled && (
              <Switch>
                <PrivateRoute path={LINKS.ORGANIZATION_SUBSCRIPTIONS.template}>
                  <OrganizationSubscriptionsView />
                </PrivateRoute>
                <PrivateRoute path={LINKS.ORGANIZATION_INVOICES.template}>
                  <OrganizationInvoicesView />
                </PrivateRoute>
                <PrivateRoute path={LINKS.ORGANIZATION_BILLING.template}>
                  <OrganizationBillingView />
                </PrivateRoute>
                {config.internalControllerEnabled && (
                  <PrivateRoute
                    path={LINKS.ORGANIZATION_BILLING_TEST_CLOCK_HELPER.template}
                  >
                    <OrganizationBillingTestClockHelperView />
                  </PrivateRoute>
                )}
              </Switch>
            )}
          </>
        );
      },
      Project: () => (
        <Switch>
          <Route path={LINKS.PROJECT_TASKS.template}>
            <ProjectTasksView />
          </Route>
        </Switch>
      ),
    },
    tasks: {
      useUserTaskCount,
      TranslationTaskIndicator,
      PrefilterTask,
      TranslationsTaskDetail,
    },

    useAddBatchOperations: () => {
      const { satisfiesPermission } = useProjectPermissions();
      const prefilteredTask = useTranslationsSelector(
        (c) => c.prefilter?.task !== undefined
      );
      const { features } = useEnabledFeatures();
      const canEditTasks = satisfiesPermission('tasks.edit');
      const taskFeature = features.includes('TASKS');
      const { t } = useTranslate();

      return addOperations(
        [
          {
            id: 'task_create',
            label: t('batch_operations_create_task'),
            divider: true,
            enabled: canEditTasks,
            hidden: !taskFeature,
            component: OperationTaskCreate,
          },
          {
            id: 'task_add_keys',
            label: t('batch_operations_task_add_keys'),
            enabled: canEditTasks,
            hidden: prefilteredTask || !taskFeature,
            component: OperationTaskAddKeys,
          },
          {
            id: 'task_remove_keys',
            label: t('batch_operations_task_remove_keys'),
            enabled: canEditTasks,
            hidden: !prefilteredTask || !taskFeature,
            component: OperationTaskRemoveKeys,
          },
        ],
        { position: 'after', value: 'export_translations' }
      );
    },
    translationPanelAdder: addPanel(
      [
        {
          id: 'tasks',
          icon: <ClipboardCheck />,
          name: <T keyName="translation_tools_tasks" />,
          component: TasksPanel,
          itemsCountFunction: tasksCount,
          displayPanel: ({ projectPermissions }) =>
            projectPermissions.satisfiesPermission('tasks.view'),
          hideWhenCountZero: true,
          hideCount: true,
        },
      ],
      { position: 'after', value: 'history' }
    ),
    useAddDeveloperViewItems: () => {
      const { t } = useTranslate();
      return addDeveloperViewItems(
        [
          {
            value: 'storage',
            tab: {
              label: t('developer_menu_storage'),
              dataCy: 'developer-menu-storage',
              condition: ({ satisfiesPermission }) =>
                satisfiesPermission('content-delivery.manage'),
            },
            link: LINKS.PROJECT_STORAGE,
            component: StorageList,
          },
          {
            value: 'webhooks',
            tab: {
              label: t('developer_menu_webhooks'),
              dataCy: 'developer-menu-webhooks',
              condition: ({ satisfiesPermission }) =>
                satisfiesPermission('webhooks.manage'),
            },
            link: LINKS.PROJECT_WEBHOOKS,
            component: WebhookList,
          },
        ],
        { position: 'after', value: 'content-delivery' }
      );
    },
    useAddUserMenuItems: () => {
      const taskCount = useUserTaskCount();

      return addUserMenuItems(
        [
          {
            Component: ({ onClose }) => {
              const { t } = useTranslate();

              return (
                <MenuItem
                  component={Link}
                  to={LINKS.MY_TASKS.build()}
                  selected={location.pathname === LINKS.MY_TASKS.build()}
                  onClick={onClose}
                  data-cy="user-menu-my-tasks"
                  sx={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    paddingRight: 3,
                  }}
                >
                  <Box>{t('user_menu_my_tasks')}</Box>
                  <Badge badgeContent={taskCount} color="primary" />
                </MenuItem>
              );
            },
            enabled: true,
            id: 'mu-tasks',
          },
        ],
        { position: 'start' }
      );
    },
    useAddProjectMenuItems: () => {
      const { t } = useTranslate();

      return addProjectMenuItems(
        [
          {
            id: 'tasks',
            condition: ({ satisfiesPermission }) =>
              satisfiesPermission('tasks.view'),
            link: LINKS.PROJECT_TASKS,
            icon: ClipboardCheck,
            text: t('project_menu_tasks'),
            dataCy: 'project-menu-item-tasks',
            matchAsPrefix: true,
          },
        ],
        {
          position: 'after',
          value: 'translations',
        }
      );
    },
  },
};

function useUserTaskCount() {
  const userInfo = useGlobalContext((context) => context.initialData.userInfo);
  const loadable = useUserTasks({ enabled: !!userInfo });
  return loadable.data?.page?.totalElements ?? 0;
}
