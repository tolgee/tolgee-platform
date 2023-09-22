import { useState } from 'react';
import { useRouteMatch } from 'react-router-dom';
import {
  HighlightItem,
  ItemStep,
} from 'tg.component/layout/QuickStartGuide/types';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import type { useInitialDataService } from './useInitialDataService';

export const useQuickStartGuide = (
  initialData: ReturnType<typeof useInitialDataService>
) => {
  const [active, setActive] = useState<HighlightItem[]>([]);
  const [activeStep, setActiveStep] = useState<ItemStep>();
  const match = useRouteMatch(LINKS.PROJECT.template);
  const projectIdParam = match?.params[PARAMS.PROJECT_ID];
  const projectId = isNaN(projectIdParam) ? undefined : projectIdParam;

  const organizationSlug = initialData.data.preferredOrganization?.slug;
  const isOwner =
    initialData.data.preferredOrganization?.currentUserRole === 'OWNER';

  const projects = useApiQuery({
    url: '/v2/organizations/{slug}/projects',
    method: 'get',
    path: { slug: organizationSlug! },
    query: { size: 1, sort: ['id,desc'] },
    options: {
      enabled: projectId === undefined && Boolean(organizationSlug),
    },
  });

  const lastProjectId = projectId
    ? projectId
    : projects.data?._embedded?.projects?.[0]?.id;

  const completed = initialData.data.userInfo?.quickStart?.completedSteps || [];

  const allCompleted =
    lastProjectId === undefined
      ? [...completed]
      : ['new_project', ...completed];

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
    initialData.completeGuideStep(item);
  }

  function skipTips() {
    if (activeStep) {
      quickStartCompleteStep(activeStep);
      setActiveStep(undefined);
      setActive([]);
    }
  }

  const state = {
    open: initialData.data.userInfo?.quickStart?.open && isOwner,
    active: active[0],
    lastProjectId,
    completed: allCompleted,
  };

  const actions = {
    quickStartDismiss: initialData.dismissGuide,
    quickStartBegin,
    quickStartVisited,
    quickStartCompleteStep,
    quickStartSkipTips: skipTips,
  };

  return [state, actions] as const;
};
