import { useState } from 'react';
import { useTranslate } from '@tolgee/react';
import { useProject } from 'tg.hooks/useProject';
import { useEnabledFeatures, useUser } from 'tg.globalContext/helpers';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import {
  ACTION_REQUIREMENTS,
  ACTIVITY_ACTIONS,
  ACTIVITY_CATEGORIES,
  ActivityAction,
  ActivityCategory,
} from './activityCategories';
import {
  useActivityActionHints,
  useActivityActionLabels,
} from './useActivityActionLabels';
import { useActivityCategoryLabels } from './useActivityCategoryLabels';
import {
  MultiSelectOption,
  MultiSelectPreset,
  TreeGroup,
} from './ActivityFilterOptions';

const NONE = 'NONE';

export const sameSet = (a: string[], b: string[]) =>
  a.length === b.length && a.every((v) => b.includes(v));

const EXCLUDE = '!';

const decodeSelection = (param: string[], all: string[]) => {
  if (param.length === 0) return all;
  if (param[0] === NONE) return [];
  if (param[0].startsWith(EXCLUDE)) {
    const excluded = param.map((v) => v.slice(EXCLUDE.length));
    return all.filter((v) => !excluded.includes(v));
  }
  return param.filter((v) => all.includes(v));
};

const encodeSelection = (selected: string[], all: string[]) => {
  if (sameSet(selected, all)) return [];
  if (selected.length === 0) return [NONE];
  const missing = all.filter((v) => !selected.includes(v));
  return missing.length < selected.length
    ? missing.map((v) => EXCLUDE + v)
    : selected;
};

/** Empty list = everything, `['NONE']` = nothing, otherwise the explicit selection. */
export type ActivityFilter = {
  users: string[];
  activities: string[];
  languages: string[];
};

export const DEFAULT_ACTIVITY_FILTER: ActivityFilter = {
  users: [],
  activities: [],
  languages: [],
};

export type ActivityFilterSelect = {
  value: string[];
  onChange: (value: string[]) => void;
  presets: MultiSelectPreset[];
  options: MultiSelectOption[];
  noneLabel: string;
};

export type ActivityTreeSelect = {
  value: string[];
  onChange: (value: string[]) => void;
  presets: MultiSelectPreset[];
  groups: TreeGroup[];
  expanded: string[];
  onToggleExpanded: (group: string) => void;
  summary: string;
};

export const useActivityFilter = (
  filter: ActivityFilter,
  setFilter: (filter: ActivityFilter) => void
) => {
  const { t } = useTranslate();
  const project = useProject();
  const user = useUser();
  const categoryLabels = useActivityCategoryLabels();
  const actionLabels = useActivityActionLabels();
  const actionHints = useActivityActionHints();

  const members = useApiQuery({
    url: '/v2/projects/{projectId}/users',
    method: 'get',
    path: { projectId: project.id },
    query: { size: 1000 },
  });
  const languages = useApiQuery({
    url: '/v2/projects/{projectId}/languages',
    method: 'get',
    path: { projectId: project.id },
    query: { size: 1000 },
  });

  const memberList = members.data?._embedded?.users ?? [];
  const languageList = languages.data?._embedded?.languages ?? [];
  const allUsers = memberList.map((m) => String(m.id));
  const allButMe = allUsers.filter((id) => id !== String(user?.id));
  const { isEnabled } = useEnabledFeatures();
  const { satisfiesPermission } = useProjectPermissions();
  const bounds = useApiQuery({
    url: '/v2/projects/{projectId}/activity/stream/bounds',
    method: 'get',
    path: { projectId: project.id },
  });
  const presentTypes = bounds.data?.presentTypes ?? [];
  const isActionVisible = (action: string) => {
    const req = ACTION_REQUIREMENTS[action as ActivityAction];
    if (!req) return true;
    if (req.permission && !satisfiesPermission(req.permission)) return false;
    const availableNow =
      (!req.feature || isEnabled(req.feature)) &&
      (!req.project || req.project(project));
    const usedBefore = Object.values(ACTIVITY_ACTIONS)
      .flatMap((actions) =>
        Object.entries(actions as Record<string, readonly string[]>)
      )
      .find(([a]) => a === action)?.[1]
      .some((type) => presentTypes.includes(type));
    return availableNow || Boolean(usedBefore);
  };
  const visibleActions = Object.fromEntries(
    Object.entries(ACTIVITY_ACTIONS).map(([c, actions]) => [
      c,
      Object.fromEntries(
        Object.entries(actions).filter(([action]) => isActionVisible(action))
      ),
    ])
  ) as Record<ActivityCategory, Record<string, readonly string[]>>;
  const visibleTypes = (c: ActivityCategory) =>
    Object.values(visibleActions[c]).flat();
  const allActivities = (
    Object.keys(ACTIVITY_ACTIONS) as ActivityCategory[]
  ).filter((c) => visibleTypes(c).length > 0);
  const allTypes = allActivities.flatMap(visibleTypes);
  const someHidden =
    allTypes.length <
    (Object.values(ACTIVITY_CATEGORIES) as (readonly string[])[]).flat().length;
  const [expanded, setExpanded] = useState<string[]>([]);
  const allTags = languageList.map((l) => l.tag);

  const selectedUsers = decodeSelection(filter.users, allUsers);
  const selectedTypes = decodeSelection(filter.activities, allTypes);
  const selectedTags = decodeSelection(filter.languages, allTags);

  const nothingSelected = [filter.users, filter.activities, filter.languages]
    .map((p) => p[0])
    .includes(NONE);
  const exceptMe =
    !!user && filter.users.length > 0 && sameSet(selectedUsers, allButMe);

  const query = {
    authorIds: nothingSelected
      ? [-1]
      : filter.users.length && !exceptMe
      ? selectedUsers.map(Number)
      : undefined,
    excludeAuthorIds: exceptMe && user ? [user.id] : undefined,
    types: filter.activities.length || someHidden ? selectedTypes : undefined,
    languageIds: filter.languages.length
      ? languageList
          .filter((l) => selectedTags.includes(l.tag))
          .map((l) => l.id)
      : undefined,
  };

  const users: ActivityFilterSelect = {
    value: selectedUsers,
    onChange: (v) =>
      setFilter({ ...filter, users: encodeSelection(v, allUsers) }),
    noneLabel: t('activity_stream_no_users', 'No users'),
    presets: [
      { label: t('activity_stream_all_users', 'All users'), values: allUsers },
      {
        label: t('activity_stream_except_me', 'Everyone except me'),
        values: allButMe,
      },
    ],
    options: memberList.map((m) => ({
      value: String(m.id),
      label:
        m.id === user?.id
          ? t('activity_filter_me', '{name} (you)', { name: m.name })
          : m.name,
    })),
  };

  const fullGroups = allActivities.filter((c) =>
    visibleTypes(c).every((type) => selectedTypes.includes(type))
  );
  const allActions = allActivities.flatMap((c) =>
    Object.values(visibleActions[c])
  );
  const isActionSelected = (types: readonly string[]) =>
    types.every((type) => selectedTypes.includes(type));
  const selectedActions = allActions.filter(isActionSelected).length;
  const singleAction =
    selectedActions === 1
      ? allActivities
          .flatMap((c) => Object.entries(visibleActions[c]))
          .find(([, types]) => isActionSelected(types))?.[0]
      : undefined;
  const looseActions = allActivities
    .filter((c) => !fullGroups.includes(c))
    .flatMap((c) => Object.values(visibleActions[c]))
    .filter(isActionSelected).length;

  const activities: ActivityTreeSelect = {
    value: selectedTypes,
    onChange: (v) =>
      setFilter({ ...filter, activities: encodeSelection(v, allTypes) }),
    presets: [
      {
        label: t('activity_stream_all_activities', 'All activities'),
        values: allTypes,
      },
    ],
    groups: allActivities.map((c) => ({
      value: c,
      label: categoryLabels[c],
      children: Object.entries(visibleActions[c]).map(([action, types]) => ({
        value: action,
        label: actionLabels[action as ActivityAction],
        hint: actionHints[action as ActivityAction],
        types: [...types] as string[],
      })),
    })),
    expanded,
    onToggleExpanded: (group) =>
      setExpanded((e) =>
        e.includes(group) ? e.filter((g) => g !== group) : [...e, group]
      ),
    summary: !selectedTypes.length
      ? t('activity_stream_no_activities', 'No activities')
      : selectedTypes.length === allTypes.length
      ? t('activity_stream_all_activities', 'All activities')
      : singleAction && !fullGroups.length
      ? actionLabels[singleAction as ActivityAction]
      : allActions.length - selectedActions < selectedActions
      ? t(
          'activity_filter_all_except',
          'All except {count, plural, one {# activity} other {# activities}}',
          { count: allActions.length - selectedActions }
        )
      : [
          ...fullGroups.map((c) => categoryLabels[c]),
          ...(looseActions
            ? [
                t(
                  'activity_filter_activities_count',
                  '{count, plural, one {# activity} other {# activities}}',
                  { count: looseActions }
                ),
              ]
            : []),
        ].join(', '),
  };

  const languagesSelect = {
    languages: languageList,
    value: selectedTags,
    onChange: (v: string[]) =>
      setFilter({ ...filter, languages: encodeSelection(v, allTags) }),
  };

  const isActive =
    filter.users.length + filter.activities.length + filter.languages.length >
    0;

  return {
    query,
    users,
    activities,
    languages: languagesSelect,
    isActive,
  };
};
