import { useEffect, useMemo, useState } from 'react';
import ReactList from 'react-list';
import { TolgeeFormat } from '@tginternal/editor';

import { useApiQuery } from 'tg.service/http/useQueryApi';
import { createProvider } from 'tg.fixtures/createProvider';
import { projectPreferencesService } from 'tg.service/ProjectPreferencesService';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { useUrlSearchArray } from 'tg.hooks/useUrlSearch';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import {
  AddTag,
  AddTranslation,
  CellPosition,
  ChangeScreenshotNum,
  ChangeValue,
  Edit,
  EditorProps,
  Filters,
  KeyElement,
  KeyUpdateData,
  RemoveTag,
  SetTaskTranslationState,
  SetTranslationState,
  UpdateTranslation,
  ViewMode,
} from './types';

import { useTranslationsService } from './services/useTranslationsService';
import { useEditService } from './services/useEditService';
import { useRefsService } from './services/useRefsService';
import { useTagsService } from './services/useTagsService';
import { useSelectionService } from './services/useSelectionService';
import { useStateService } from './services/useStateService';
import { useWebsocketService } from './services/useWebsocketService';
import { PrefilterType } from '../prefilters/usePrefilter';
import { useTaskService } from './services/useTaskService';
import { usePositionService } from './services/usePositionService';

type Props = {
  projectId: number;
  baseLang: string | undefined;
  keyId?: number;
  keyName?: string;
  keyNamespace?: string;
  updateLocalStorageLanguages?: boolean;
  pageSize?: number;
  prefilter?: PrefilterType;
};

export const [
  TranslationsContextProvider,
  useTranslationsActions,
  useTranslationsSelector,
] = createProvider((props: Props) => {
  const [view, setView] = useUrlSearchState('view', { defaultVal: 'LIST' });
  const [sidePanelOpen, setSidePanelOpen] = useState(false);
  const urlLanguages = useUrlSearchArray().languages;
  const requiredLanguages = urlLanguages?.length
    ? urlLanguages
    : projectPreferencesService.getForProject(props.projectId);

  const [initialLangs] = useState<string[] | null | undefined>(
    requiredLanguages
  );

  const { satisfiesLanguageAccess } = useProjectPermissions();

  const languagesLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/languages',
    method: 'get',
    path: { projectId: props.projectId },
    query: { size: 1000, sort: ['tag'] },
    options: {
      cacheTime: 0,
    },
  });

  const allowedLanguages = useMemo(
    () =>
      languagesLoadable.data?._embedded?.languages?.filter((l) =>
        satisfiesLanguageAccess('translations.view', l.id)
      ),
    [languagesLoadable.data]
  );

  const translationService = useTranslationsService({
    projectId: props.projectId,
    keyId: props.keyId,
    keyName: props.keyName,
    keyNamespace: props.keyNamespace,
    pageSize: props.pageSize,
    updateLocalStorageLanguages: props.updateLocalStorageLanguages,
    // when initial langs are null, fetching is postponed
    initialLangs: initialLangs,
    baseLang: props.baseLang,
    prefilter: props.prefilter,
  });

  const { setEventBlockers } = useWebsocketService(translationService);

  const viewRefs = useRefsService();

  const taskService = useTaskService({ translations: translationService });

  const stateService = useStateService({
    translations: translationService,
    taskService,
    prefilter: props.prefilter,
  });

  const positionService = usePositionService({
    translations: translationService,
    viewRefs,
  });

  const editService = useEditService({
    positionService,
    translationService,
    viewRefs,
    taskService,
    prefilter: props.prefilter,
  });

  const tagsService = useTagsService({
    translations: translationService,
  });

  const selectionService = useSelectionService({
    translations: translationService,
  });

  const handleTranslationsReset = () => {
    positionService.clearPosition();
    selectionService.clear();
  };

  useEffect(() => {
    // prevent leaving the page when there are unsaved changes
    if (positionService.position?.changed) {
      const handler = (e) => {
        e.preventDefault();
        e.returnValue = '';
      };
      window.addEventListener('beforeunload', handler);
      return () => window.removeEventListener('beforeunload', handler);
    }
  }, [positionService.position?.changed]);

  // actions

  const actions = {
    setSearch(search: string) {
      translationService.setSearch(search);
      return handleTranslationsReset();
    },
    setSearchImmediate(search: string) {
      translationService.setUrlSearch(search);
      return handleTranslationsReset();
    },
    async setFilters(filters: Filters) {
      if (await positionService.confirmUnsavedChanges()) {
        translationService.setFilters(filters);
        return handleTranslationsReset();
      }
    },
    async setEdit(edit: EditorProps | undefined) {
      if (await positionService.confirmUnsavedChanges(edit)) {
        setSidePanelOpen(true);
        return positionService.setPositionAndFocus(edit);
      }
    },
    async setEditValue(value: TolgeeFormat) {
      setSidePanelOpen(true);
      editService.setEditValue(value);
    },
    async setEditValueString(value: string) {
      setSidePanelOpen(true);
      editService.setEditValueString(value);
    },
    setEditForce(edit: EditorProps | undefined) {
      return positionService.setPositionAndFocus(edit);
    },
    async updateEdit(edit: Partial<Edit>) {
      if (await positionService.confirmUnsavedChanges(edit)) {
        return positionService.updatePosition(edit);
      }
    },
    toggleSelect(index: number) {
      return selectionService.toggle(index);
    },
    async selectAll() {
      const allItems = await translationService.getAllIds();
      return selectionService.select(allItems.ids);
    },
    selectionClear() {
      return selectionService.clear();
    },
    fetchMore() {
      return translationService.fetchNextPage();
    },
    changeField(value: ChangeValue) {
      return editService.changeField(value);
    },
    selectLanguages(languages: string[] | undefined) {
      translationService.setLanguages(languages);
      return handleTranslationsReset();
    },
    updateLanguages() {
      translationService.updateQuery({});
      return handleTranslationsReset();
    },
    updateScreenshotCount(count: ChangeScreenshotNum) {
      return translationService.updateScreenshotCount(count);
    },
    changeView(view: ViewMode) {
      return setView(view);
    },
    setTranslationState(state: SetTranslationState) {
      return stateService.changeState(state);
    },
    setTaskState(state: SetTaskTranslationState) {
      return taskService.setTaskTranslationState(state);
    },
    addTag(tag: AddTag) {
      return tagsService.addTag(tag);
    },
    removeTag(tag: RemoveTag) {
      return tagsService.removeTag(tag);
    },
    updateTranslation(translation: UpdateTranslation) {
      return translationService.updateTranslation(translation);
    },
    updateKey(updateKey: KeyUpdateData) {
      return translationService.updateTranslationKeys([updateKey]);
    },
    insertTranslation(translation: AddTranslation) {
      return translationService.insertAsFirst(translation);
    },
    registerElement(element: KeyElement) {
      return viewRefs.registerElement(element);
    },
    unregisterElement(element: KeyElement) {
      return viewRefs.unregisterElement(element);
    },
    focusElement(element: CellPosition) {
      return viewRefs.focusCell(element);
    },
    registerList(list: ReactList) {
      return viewRefs.registerList(list);
    },
    unregisterList(list: ReactList) {
      return viewRefs.unregisterList(list);
    },
    refetchTranslations() {
      return translationService.refetchTranslations();
    },
    setSidePanelOpen,
    setEventBlockers,
  };

  const dataReady = Boolean(
    languagesLoadable.data && translationService.fixedTranslations
  );

  const state = {
    baseLanguage: props.baseLang!,
    dataReady,
    // changes immediately when user clicks
    selectedLanguages: translationService.selectedLanguages,
    translations: dataReady ? translationService.fixedTranslations : undefined,
    // changes after translations are loaded
    translationsLanguages: translationService.translationsLanguages,
    translationsTotal:
      translationService.totalCount !== undefined
        ? translationService.totalCount
        : undefined,
    languages: dataReady ? allowedLanguages : undefined,
    isLoading: translationService.isLoading || languagesLoadable.isLoading,
    isFetching:
      translationService.isFetching ||
      languagesLoadable.isFetching ||
      stateService.isLoading ||
      tagsService.isLoading,
    isEditLoading: editService.isLoading,
    isFetchingMore: translationService.isFetchingNextPage,
    isLoadingAllIds: translationService.isLoadingAllIds,
    hasMoreToFetch: translationService.hasNextPage,
    search: translationService.search as string,
    urlSearch: translationService.urlSearch,
    filters: translationService.filters,
    cursor: positionService.position,
    selection: selectionService.data,
    view: view as ViewMode,
    elementsRef: viewRefs.elementsRef,
    reactList: viewRefs.reactList,
    sidePanelOpen,
    prefilter: props.prefilter,
  };

  return [state, actions];
});
