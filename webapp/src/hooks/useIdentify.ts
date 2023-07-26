import { useApiMutation } from 'tg.service/http/useQueryApi';
import { AnonymousIdService } from 'tg.service/AnonymousIdService';
import { useEffect } from 'react';

export const useIdentify = (userId?: number) => {
  const mutation = useApiMutation({
    url: '/v2/public/business-events/identify',
    method: 'post',
    options: {
      onSuccess() {
        AnonymousIdService.dispose();
      },
    },
  });

  useEffect(() => {
    const anonymousId = AnonymousIdService.get();
    const enabled = !!anonymousId && !!userId;
    if (enabled) {
      mutation.mutate({
        content: {
          'application/json': {
            anonymousUserId: anonymousId!,
          },
        },
      });
    }
  }, [userId]);
};
