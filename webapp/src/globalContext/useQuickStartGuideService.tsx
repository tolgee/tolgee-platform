import { useMediaQuery } from '@mui/material';
import { useState } from 'react';
import { useRouteMatch } from 'react-router-dom';
import {
  HighlightItem,
  ItemStep,
} from 'tg.component/layout/QuickStartGuide/enums';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import type { useInitialDataService } from './useInitialDataService';

export const useQuickStartGuideService = (
  initialData: ReturnType<typeof useInitialDataService>
) => {
  const [active, setActive] = useState<HighlightItem[]>([]);
  const [activeStep, setActiveStep] = useState<ItemStep>();
  const [floatingForced, setFloatingForced] = useState(false);
  const floating =
    useMediaQuery(`@media (max-width: ${1200}px)`) || floatingForced;
  const match = useRouteMatch(LINKS.PROJECT.template);
  const projectIdParam = match?.params[PARAMS.PROJECT_ID];
  const projectId = isNaN(projectIdParam) ? undefined : projectIdParam;
  const [floatingOpen, setFloatingOpen] = useState(false);
  const isVerified =
    initialData.state?.userInfo?.emailAwaitingVerification === null ||
    !initialData.state?.serverConfiguration.needsEmailVerification;
  const organizationSlug = initialData.state?.preferredOrganization?.slug;
  const isOwner =
    initialData.state?.preferredOrganization?.currentUserRole === 'OWNER';

  const projects = useApiQuery({
    url: '/v2/organizations/{slug}/projects',
    method: 'get',
    path: { slug: organizationSlug! },
    query: { size: 1, sort: ['id,desc'] },
    options: {
      enabled:
        projectId === undefined && Boolean(organizationSlug) && isVerified,
    },
  });

  const lastProjectId = projectId
    ? projectId
    : projects.data?._embedded?.projects?.[0]?.id;

  const completed =
    initialData.state?.preferredOrganization?.quickStart?.completedSteps || [];

  const allCompleted = completed;

  function quickStartBegin(step: ItemStep, items: HighlightItem[]) {
    setActiveStep(step);
    setActive(items);
  }

  function quickStartVisited(item: HighlightItem) {
    const activeItems = active[0] === item ? active.slice(1) : active;
    setActive(activeItems);
    if (activeItems.length === 0 && activeStep) {
      quickStartCompleteStep(activeStep);
      setActiveStep(undefined);
    }
  }

  function quickStartCompleteStep(item: ItemStep) {
    if (enabled) {
      initialData.actions.completeGuideStep(item);
    }
  }

  function skipTips() {
    if (activeStep) {
      quickStartCompleteStep(activeStep);
      setActiveStep(undefined);
      setActive([]);
    }
  }

  const enabled =
    initialData.state?.preferredOrganization?.quickStart?.finished === false &&
    isOwner;

  const open =
    enabled &&
    (floating
      ? floatingOpen
      : initialData.state?.preferredOrganization?.quickStart?.open);

  const state = {
    enabled,
    open,
    active: active[0],
    lastProjectId,
    completed: allCompleted,
    floating,
  };

  const actions = {
    quickStartFinish: initialData.actions.finishGuide,
    setQuickStartOpen: floating
      ? setFloatingOpen
      : initialData.actions.setQuickStartOpen,
    quickStartBegin,
    quickStartVisited,
    quickStartCompleteStep,
    quickStartSkipTips: skipTips,
    quickStartForceFloating: setFloatingForced,
  };

  return { state, actions };
};
