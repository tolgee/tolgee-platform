import { PluginType } from './plugin/PluginType';
import { PermissionsAdvancedEe } from 'tg.ee/PermissionsAdvanced/PermissionsAdvancedEe';
import { BillingMenuItem } from 'tg.ee/billing/component/UserMenu/BillingMenuItem';
import { PrivateRoute } from 'tg.component/common/PrivateRoute';
import { LINKS } from 'tg.constants/links';
import React from 'react';
import { MyTasksView } from 'tg.ee/task/views/myTasks/MyTasksView';
import { TaskReference } from 'tg.ee/task/components/TaskReference';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';
import { useUserTasks } from 'tg.globalContext/useUserTasks';
import { AdministrationCloudPlansView } from 'tg.ee/billing/administration/AdministrationCloudPlansView';
import { AdministrationCloudPlanCreateView } from 'tg.ee/billing/administration/AdministrationCloudPlanCreateView';
import { AdministrationCloudPlanEditView } from 'tg.ee/billing/administration/AdministrationCloudPlanEditView';
import { AdministrationEePlansView } from 'tg.ee/billing/administration/AdministrationEePlansView';
import { AdministrationEePlanCreateView } from 'tg.ee/billing/administration/AdministrationEePlanCreateView';
import { AdministrationEePlanEditView } from 'tg.ee/billing/administration/AdministrationEePlanEditView';
import { AdministrationEeLicenseView } from 'tg.ee/billing/administration/AdministrationEeLicenseView';
import { SlackApp } from 'tg.ee/organizationApps/SlackApp';
import { Usage } from 'tg.ee/billing/component/Usage';
import { useConfig, useEnabledFeatures } from 'tg.globalContext/helpers';
import { OrganizationSubscriptionsView } from 'tg.ee/billing/Subscriptions/OrganizationSubscriptionsView';
import { OrganizationInvoicesView } from 'tg.ee/billing/Invoices/OrganizationInvoicesView';
import { OrganizationBillingView } from 'tg.ee/billing/OrganizationBillingView';
import { OrganizationBillingTestClockHelperView } from 'tg.ee/billing/OrganizationBillingTestClockHelperView';
import { Route, Switch } from 'react-router-dom';
import { ProjectTasksView } from 'tg.ee/task/views/projectTasks/ProjectTasksView';
import { addOperationAfter } from 'tg.views/projects/translations/BatchOperations/operations';
import { OperationTaskCreate } from 'tg.ee/batchOperations/OperationTaskCreate';
import { OperationTaskAddKeys } from 'tg.ee/batchOperations/OperationTaskAddKeys';
import { OperationTaskRemoveKeys } from 'tg.ee/batchOperations/OperationTaskRemoveKeys';
import { useTranslationsSelector } from 'tg.views/projects/translations/context/TranslationsContext';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { T, useTranslate } from '@tolgee/react';
import { TranslationTaskIndicator } from 'tg.ee/task/components/TranslationTaskIndicator';
import { PrefilterTask } from 'tg.ee/task/components/PrefilterTask';
import { addPanelAfter } from 'tg.views/projects/translations/ToolsPanel/panelsList';
import { ClipboardCheck } from '@untitled-ui/icons-react';
import { tasksCount, TasksPanel } from 'tg.ee/task/components/TasksPanel';
import { TranslationsTaskDetail } from 'tg.ee/task/components/TranslationsTaskDetail';

export const eePlugin: PluginType = {
  ee: {
    activity: {
      TaskReference: TaskReference,
    },
    PermissionsAdvanced: PermissionsAdvancedEe,
    billing: {
      billingMenuItems: [BillingMenuItem],
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
      useUserTaskCount: () => {
        const userInfo = useGlobalContext(
          (context) => context.initialData.userInfo
        );
        const loadable = useUserTasks({ enabled: !!userInfo });
        return loadable.data?.page?.totalElements ?? 0;
      },
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

      return addOperationAfter(
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
        'export_translations'
      );
    },
    translationPanelAdder: addPanelAfter(
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
      'history'
    ),
  },
};
