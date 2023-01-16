import { useState } from 'react';
import ReactList from 'react-list';
import { useApiQuery } from 'tg.service/http/useQueryApi';

import { createProviderNew } from 'tg.fixtures/createProviderNew';
import { container } from 'tsyringe';
import { ProjectPreferencesService } from 'tg.service/ProjectPreferencesService';
import { useTranslationsService } from './services/useTranslationsService';
import { useEditService } from './services/useEditService';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import {
  AddTag,
  AddTranslation,
  CellPosition,
  ChangeScreenshotNum,
  ChangeValue,
  Edit,
  Filters,
  KeyElement,
  KeyUpdateData,
  RemoveTag,
  ScrollToElement,
  SetTranslationState,
  UpdateTranslation,
  ViewMode,
} from './types';
import { useRefsService } from './services/useRefsService';
import { useTagsService } from './services/useTagsService';
import { useSelectionService } from './services/useSelectionService';
import { useStateService } from './services/useStateService';
import { useUrlSearchArray } from 'tg.hooks/useUrlSearch';
import { useWebsocketListener } from './services/useWebsocketListener';

const projectPreferences = container.resolve(ProjectPreferencesService);

type Props = {
  projectId: number;
  baseLang: string | undefined;
  keyId?: number;
  keyName?: string;
  keyNamespace?: string;
  updateLocalStorageLanguages?: boolean;
  pageSize?: number;
};

export const [
  TranslationsContextProvider,
  useTranslationsActions,
  useTranslationsSelector,
] = createProviderNew((props: Props) => {
  const [view, setView] = useUrlSearchState('view', { defaultVal: 'LIST' });
  const [initialLangs, setInitialLangs] = useState<string[] | null | undefined>(
    null
  );

  const urlLanguages = useUrlSearchArray().languages;

  const requiredLanguages = urlLanguages?.length
    ? urlLanguages
    : projectPreferences.getForProject(props.projectId);

  const languages = useApiQuery({
    url: '/v2/projects/{projectId}/languages',
    method: 'get',
    path: { projectId: props.projectId },
    query: { size: 1000, sort: ['tag'] },
    options: {
      onSuccess(data) {
        const languages = requiredLanguages?.filter((l) =>
          data._embedded?.languages?.find((lang) => lang.tag === l)
        );
        // manually set initial langs
        setInitialLangs(languages.length ? languages : undefined);
      },
      cacheTime: 0,
    },
  });

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
  });

  useWebsocketListener(translationService);

  const viewRefs = useRefsService();

  const editService = useEditService({
    translations: translationService,
    viewRefs,
  });

  const tagsService = useTagsService({
    translations: translationService,
  });

  const selectionService = useSelectionService({
    translations: translationService,
  });

  const stateService = useStateService({ translations: translationService });

  const handleTranslationsReset = () => {
    editService.setPosition(undefined);
    selectionService.clear();
  };

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
    setFilters(filters: Filters) {
      translationService.setFilters(filters);
      return handleTranslationsReset();
    },
    setEdit(edit: Edit | undefined) {
      return editService.setEdit(edit);
    },
    setEditForce(edit: Edit | undefined) {
      return editService.setPositionAndFocus(edit);
    },
    updateEdit(edit: Partial<Edit>) {
      return editService.updatePosition(edit);
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
    async deleteTranslations() {
      await selectionService.deleteSelected();
    },
    setTranslationState(state: SetTranslationState) {
      return stateService.changeState(state);
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
    scrollToElement(element: ScrollToElement) {
      return viewRefs.scrollToElement(element);
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
  };

  const dataReady = Boolean(
    languages.data && translationService.fixedTranslations
  );

  const state = {
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
    languages: dataReady ? languages.data?._embedded?.languages : undefined,
    isLoading: translationService.isLoading || languages.isLoading,
    isFetching:
      translationService.isFetching ||
      languages.isFetching ||
      stateService.isLoading ||
      tagsService.isLoading,
    isEditLoading: editService.isLoading,
    isFetchingMore: translationService.isFetchingNextPage,
    isLoadingAllIds: translationService.isLoadingAllIds,
    isDeleting: selectionService.isDeleting,
    hasMoreToFetch: translationService.hasNextPage,
    search: translationService.search as string,
    urlSearch: translationService.urlSearch,
    filters: translationService.filters,
    cursor: editService.position,
    selection: selectionService.data,
    view: view as ViewMode,
    elementsRef: viewRefs.elementsRef,
    reactList: viewRefs.reactList,
  };

  return [state, actions];
});
