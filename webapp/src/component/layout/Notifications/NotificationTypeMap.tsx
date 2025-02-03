import { TaskAssignedItem } from 'tg.component/layout/Notifications/TaskAssignedItem';
import { TaskCompletedItem } from 'tg.component/layout/Notifications/TaskCompletedItem';
import { TaskClosedItem } from 'tg.component/layout/Notifications/TaskClosedItem';
import { MfaEnabledItem } from 'tg.component/layout/Notifications/MfaEnabledItem';
import { MfaDisabledItem } from 'tg.component/layout/Notifications/MfaDisabledItem';
import { PasswordChangedItem } from 'tg.component/layout/Notifications/PasswordChangedItem';
import { components } from 'tg.service/apiSchema.generated';
import { NotificationItemProps } from 'tg.component/layout/Notifications/NotificationItem';
import React from 'react';

type NotificationsComponentMap = Record<
  components['schemas']['NotificationModel']['type'],
  React.FC<NotificationItemProps>
>;

export const notificationComponents: NotificationsComponentMap = {
  TASK_ASSIGNED: TaskAssignedItem,
  TASK_COMPLETED: TaskCompletedItem,
  TASK_CLOSED: TaskClosedItem,
  MFA_ENABLED: MfaEnabledItem,
  MFA_DISABLED: MfaDisabledItem,
  PASSWORD_CHANGED: PasswordChangedItem,
};
