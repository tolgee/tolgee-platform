import { useEffect, useMemo, useRef, useState } from 'react';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';
import { ContextOrganizationModel } from 'tg.globalContext/types';
import { useTolgee } from '@tolgee/react';
import {
  SwitchHandlers,
  OrganizationSwitchTo,
  createOrganizationSwitchSequencer,
} from 'tg.globalContext/organizationSwitchSequencer';
import {
  isSwitchInProgress,
  noSwitchInProgress,
  switchProgress,
} from 'tg.fixtures/switchProgress';

type PrivateOrganizationModel =
  components['schemas']['PrivateOrganizationModel'];

type AnnouncementDto = components['schemas']['AnnouncementDto'];
type QuickStartModel = components['schemas']['QuickStartModel'];
type InitialDataModel = components['schemas']['InitialDataModel'];

export const useInitialDataService = () => {
  const [progress, setProgress] = useState(noSwitchInProgress);
  const switchHandlersRef = useRef<SwitchHandlers<PrivateOrganizationModel>>(
    undefined as never
  );
  const switchToRef = useRef<OrganizationSwitchTo | undefined>(undefined);
  if (!switchToRef.current) {
    switchToRef.current = createOrganizationSwitchSequencer({
      write: (organizationId) =>
        switchHandlersRef.current.write(organizationId),
      apply: (data) => switchHandlersRef.current.apply(data),
      onRequested: (request) => switchHandlersRef.current.onRequested(request),
      onSettled: (request) => switchHandlersRef.current.onSettled(request),
    });
  }
  const switchTo = switchToRef.current;
  const isSwitchingOrganization = isSwitchInProgress(progress);
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
      if (initialData.languageTag) {
        // switch ui language, once user is signed in
        tolgee.changeLanguage(initialData.languageTag);
      }
    }
  }, [Boolean(initialData)]);

  const setPreferredOrganization = useApiMutation({
    url: '/v2/user-preferences/set-preferred-organization/{organizationId}',
    method: 'put',
    options: { noGlobalLoading: true },
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

  switchHandlersRef.current = {
    write: (organizationId: number) =>
      setPreferredOrganization.mutateAsync({ path: { organizationId } }),
    apply: (data) => {
      setQuickStart(data.quickStart);
      setOrganization(data);
    },
    onRequested: (request) =>
      setProgress((state) =>
        switchProgress(state, { kind: 'requested', request })
      ),
    onSettled: (request) =>
      setProgress((state) =>
        switchProgress(state, { kind: 'settled', request })
      ),
  };

  const updatePreferredOrganization = (
    organizationId: number
  ): Promise<boolean> => switchTo(organizationId, preferredOrganization?.id);

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

  const publishedPreferredOrganization: ContextOrganizationModel | undefined =
    useMemo(() => {
      if (!preferredOrganization) {
        return undefined;
      }
      const { quickStart: _quickStart, ...rest } = preferredOrganization;
      return rest;
    }, [preferredOrganization]);

  const state = initialData
    ? {
        ...initialData,
        preferredOrganization: publishedPreferredOrganization,
        quickStart,
        announcement,
        isSwitchingOrganization,
      }
    : undefined;

  return {
    error: initialDataLoadable.error,
    state,
    actions: {
      refetchInitialData,
      invalidateInitialData,
      updatePreferredOrganization,
      dismissAnnouncement,
      completeGuideStep,
      finishGuide,
      setQuickStartOpen,
    },
  };
};
