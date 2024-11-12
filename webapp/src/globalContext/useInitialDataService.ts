import { useEffect, useState } from 'react';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';
import { useTolgee } from '@tolgee/react';
import { TASK_ACTIVE_STATES } from 'tg.ee/task/components/utils';

type PrivateOrganizationModel =
  components['schemas']['PrivateOrganizationModel'];
type AnnouncementDto = components['schemas']['AnnouncementDto'];
type QuickStartModel = components['schemas']['QuickStartModel'];
type InitialDataModel = components['schemas']['InitialDataModel'];

export const useInitialDataService = () => {
  const [organizationLoading, setOrganizationLoading] = useState(false);
  const tolgee = useTolgee();

  const [organization, setOrganization] = useState<
    PrivateOrganizationModel | undefined
  >(undefined);
  const initialDataLoadable = useApiQuery({
    url: '/v2/public/initial-data',
    method: 'get',
    options: {
      cacheTime: Infinity,
      keepPreviousData: true,
      staleTime: Infinity,
      onSuccess(data) {
        setQuickStart(data.preferredOrganization?.quickStart);
        setAnnouncement(data.announcement);
        setInitialData(data);
      },
    },
    fetchOptions: {
      disable404Redirect: true,
    },
  });

  const [userTasks, setUserTasks] = useState(0);
  const userTasksLoadable = useApiQuery({
    url: '/v2/user-tasks',
    method: 'get',
    query: { size: 1, filterState: TASK_ACTIVE_STATES },
    options: {
      enabled: Boolean(initialDataLoadable.data?.userInfo),
      refetchInterval: 60_000,
    },
  });

  useEffect(() => {
    setUserTasks(userTasksLoadable.data?.page?.totalElements ?? 0);
  }, [userTasksLoadable.data]);

  const [announcement, setAnnouncement] = useState<AnnouncementDto | undefined>(
    initialDataLoadable.data?.announcement
  );
  const [quickStart, setQuickStart] = useState<QuickStartModel | undefined>(
    initialDataLoadable.data?.preferredOrganization?.quickStart
  );
  const [initialData, setInitialData] = useState<InitialDataModel | undefined>(
    initialDataLoadable.data
  );

  useEffect(() => {
    // once initial data are loaded for first time
    if (initialData) {
      // set organization data only if missing
      setOrganization((org) => (org ? org : initialData.preferredOrganization));
      if (initialData.languageTag) {
        // switch ui language, once user is signed in
        tolgee.changeLanguage(initialData.languageTag);
      }
    }
  }, [Boolean(initialData)]);

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
    organization ?? initialData?.preferredOrganization;

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
    return initialDataLoadable.refetch();
  };

  const invalidateInitialData = () => {
    setInitialData(undefined);
    return refetchInitialData();
  };

  const dismissAnnouncement = () => {
    setAnnouncement(undefined);
    dismissAnnouncementLoadable.mutate(
      {},
      {
        onError() {
          setAnnouncement(announcement);
        },
      }
    );
  };

  const isFetching =
    initialDataLoadable.isFetching ||
    setPreferredOrganization.isLoading ||
    preferredOrganizationLoadable.isLoading ||
    dismissAnnouncementLoadable.isLoading ||
    organizationLoading;

  const state = initialData
    ? {
        ...initialData!,
        preferredOrganization: preferredOrganization
          ? { ...preferredOrganization, quickStart }
          : undefined,
        announcement,
        isFetching,
        userTasks,
      }
    : undefined;

  return {
    state,
    actions: {
      refetchInitialData,
      invalidateInitialData,
      updatePreferredOrganization,
      dismissAnnouncement,
      completeGuideStep,
      finishGuide,
      setQuickStartOpen,
      setUserTasks,
    },
  };
};
