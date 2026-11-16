import { useEffect } from 'react';

import { useApiMutation } from 'tg.service/http/useQueryApi';

export const useMarkNotificationSeenOnOpen = (
  notificationId: number | undefined
) => {
  const markSeen = useApiMutation({
    url: '/v2/notifications-mark-seen',
    method: 'put',
  });

  useEffect(() => {
    if (notificationId === undefined) {
      return;
    }
    markSeen.mutate({
      content: {
        'application/json': { notificationIds: [notificationId] },
      },
    });
  }, [notificationId]);
};
