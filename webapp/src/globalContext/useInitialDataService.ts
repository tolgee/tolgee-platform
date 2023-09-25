import { useEffect, useState } from 'react';
import { useSelector } from 'react-redux';
import { container } from 'tsyringe';

import { AppState } from 'tg.store/index';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { GlobalActions } from 'tg.store/global/GlobalActions';
import { components } from 'tg.service/apiSchema.generated';
import { InvitationCodeService } from 'tg.service/InvitationCodeService';
import { useTolgee } from '@tolgee/react';
import { useOnUpdate } from 'tg.hooks/useOnUpdate';

type PrivateOrganizationModel =
  components['schemas']['PrivateOrganizationModel'];
type AnnouncementDto = components['schemas']['AnnouncementDto'];
type QuickStartModel = components['schemas']['QuickStartModel'];

export const useInitialDataService = () => {
  const [organizationLoading, setOrganizationLoading] = useState(false);
  const actions = container.resolve(GlobalActions);
  const tolgee = useTolgee();

  const [organization, setOrganization] = useState<
    PrivateOrganizationModel | undefined
  >(undefined);
  const security = useSelector((state: AppState) => state.global.security);
  const [announcement, setAnnouncement] = useState<AnnouncementDto | null>();
  const [quickStart, setQuickStart] = useState<QuickStartModel | undefined>();
  const initialData = useApiQuery({
    url: '/v2/public/initial-data',
    method: 'get',
    options: {
      refetchOnMount: false,
      cacheTime: Infinity,
      keepPreviousData: true,
      staleTime: Infinity,
    },
  });

  useEffect(() => {
    const data = initialData.data;
    if (data) {
      // set organization data only if missing
      setOrganization((org) => (org ? org : data.preferredOrganization));
      setAnnouncement(data.announcement);
      if (data.languageTag) {
        // switch ui language, once user is signed in
        tolgee.changeLanguage(data.languageTag);
      }
      const invitationCode = InvitationCodeService.getCode();
      actions.updateSecurity.dispatch({
        allowPrivate:
          !data?.serverConfiguration?.authentication || Boolean(data.userInfo),
        allowRegistration:
          data.serverConfiguration.allowRegistrations ||
          Boolean(invitationCode), // if user has invitation code, registration is allowed
      });
    }
  }, [Boolean(initialData.data)]);

  useEffect(() => {
    if (initialData.data) {
      setAnnouncement(initialData.data.announcement);
      setQuickStart(initialData.data.preferredOrganization?.quickStart);
    }
  }, [initialData.data]);

  const preferredOrganizationLoadable = useApiMutation({
    url: '/v2/preferred-organization',
    method: 'get',
  });

  const setPreferredOrganization = useApiMutation({
    url: '/v2/user-preferences/set-preferred-organization/{organizationId}',
    method: 'put',
  });

  const dismissAnnouncementLoadable = useApiMutation({
    url: '/v2/announcement/dismiss',
    method: 'post',
  });

  const putQuickStartStep = useApiMutation({
    url: '/v2/quick-start/steps/{step}/complete',
    method: 'put',
  });

  const putQuickStartFinished = useApiMutation({
    url: '/v2/quick-start/set-finished/{finished}',
    method: 'put',
  });

  const putQuickStartOpen = useApiMutation({
    url: '/v2/quick-start/set-open/{open}',
    method: 'put',
  });

  const completeGuideStep = (step: string) => {
    if (quickStart) {
      setQuickStart({
        ...quickStart,
        completedSteps: [...(quickStart.completedSteps || []), step],
      });
    }
    putQuickStartStep.mutate(
      { path: { step } },
      {
        onSuccess(data) {
          setQuickStart(data);
        },
      }
    );
  };

  const finishGuide = () => {
    if (quickStart) {
      setQuickStart({
        ...quickStart,
        finished: true,
      });
    }
    putQuickStartFinished.mutate(
      {
        path: { finished: true },
      },
      {
        onSuccess(data) {
          setQuickStart(data);
        },
      }
    );
  };

  const setQuickStartOpen = (open: boolean) => {
    if (quickStart) {
      setQuickStart({
        ...quickStart,
        open,
      });
    }
    putQuickStartOpen.mutate(
      { path: { open } },
      {
        onSuccess(data) {
          setQuickStart(data);
        },
      }
    );
  };

  const preferredOrganization =
    organization ?? initialData.data?.preferredOrganization;

  const updatePreferredOrganization = async (organizationId: number) => {
    if (organizationId !== preferredOrganization?.id) {
      setOrganizationLoading(true);
      try {
        // set preferred organization
        await setPreferredOrganization.mutateAsync({
          path: { organizationId },
        });

        // load new preferred organization
        const data = await preferredOrganizationLoadable.mutateAsync({});
        setOrganization(data);
      } finally {
        setOrganizationLoading(false);
      }
    }
  };

  const refetchInitialData = () => {
    setQuickStart(undefined);
    setOrganization(undefined);
    return initialData.refetch();
  };

  const dismissAnnouncement = () => {
    setAnnouncement(null);
    dismissAnnouncementLoadable.mutate(
      {},
      {
        onError() {
          setAnnouncement(announcement);
        },
      }
    );
  };

  useOnUpdate(() => {
    refetchInitialData();
  }, [security.jwtToken]);

  const isFetching =
    initialData.isFetching ||
    setPreferredOrganization.isLoading ||
    preferredOrganizationLoadable.isLoading ||
    dismissAnnouncementLoadable.isLoading ||
    organizationLoading;

  if (initialData.error) {
    throw initialData.error;
  }

  return {
    data: {
      ...initialData.data!,
      preferredOrganization: preferredOrganization
        ? { ...preferredOrganization, quickStart }
        : undefined,
      announcement,
    },
    isFetching,
    isLoading: initialData.isLoading,

    refetchInitialData,
    updatePreferredOrganization,
    dismissAnnouncement,
    completeGuideStep,
    finishGuide,
    setQuickStartOpen,
  };
};
