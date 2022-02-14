import { useState } from 'react';
import ReactList from 'react-list';
import { createProvider } from 'tg.fixtures/createProvider';
import { useApiQuery } from 'tg.service/http/useQueryApi';

import { container } from 'tsyringe';
import { ProjectPreferencesService } from 'tg.service/ProjectPreferencesService';
import { useTranslationsService } from './services/useTranslationsService';
import { useEditService } from './services/useEditService';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import {
  CellPosition,
  AddTag,
  AddTranslation,
  ChangeValue,
  KeyElement,
  RemoveTag,
  ScrollToElement,
  UpdateTranslation,
  ViewMode,
  Filters,
  ChangeScreenshotNum,
  SetTranslationState,
  Edit,
} from './types';
import { useRefsService } from './services/useRefsService';
import { useTagsService } from './services/useTagsService';
import { useSelectionService } from './services/useSelectionService';
import { useStateService } from './services/useStateService';

type ActionType =
  | { type: 'SET_SEARCH'; payload: string }
  | { type: 'SET_SEARCH_IMMEDIATE'; payload: string }
  | { type: 'SET_FILTERS'; payload: Filters }
  | { type: 'SET_EDIT'; payload: Edit | undefined }
  | { type: 'SET_EDIT_FORCE'; payload: Edit | undefined }
  | { type: 'UPDATE_EDIT'; payload: Partial<Edit> }
  | { type: 'TOGGLE_SELECT'; payload: number }
  | { type: 'CHANGE_FIELD'; payload: ChangeValue }
  | { type: 'FETCH_MORE' }
  | { type: 'SELECT_LANGUAGES'; payload: string[] | undefined }
  | { type: 'UPDATE_SCREENSHOT_COUNT'; payload: ChangeScreenshotNum }
  | { type: 'CHANGE_VIEW'; payload: ViewMode }
  | { type: 'UPDATE_LANGUAGES' }
  | { type: 'DELETE_TRANSLATIONS' }
  | { type: 'SET_TRANSLATION_STATE'; payload: SetTranslationState }
  | { type: 'ADD_TAG'; payload: AddTag }
  | { type: 'REMOVE_TAG'; payload: RemoveTag }
  | { type: 'UPDATE_TRANSLATION'; payload: UpdateTranslation }
  | { type: 'INSERT_TRANSLATION'; payload: AddTranslation }
  | { type: 'REGISTER_ELEMENT'; payload: KeyElement }
  | { type: 'UNREGISTER_ELEMENT'; payload: KeyElement }
  | { type: 'SCROLL_TO_ELEMENT'; payload: ScrollToElement }
  | { type: 'FOCUS_ELEMENT'; payload: CellPosition }
  | { type: 'REGISTER_LIST'; payload: ReactList }
  | { type: 'UNREGISTER_LIST'; payload: ReactList };

const projectPreferences = container.resolve(ProjectPreferencesService);

export const [
  TranslationsContextProvider,
  useTranslationsDispatch,
  useTranslationsSelector,
] = createProvider(
  (props: {
    projectId: number;
    baseLang: string | undefined;
    keyName?: string;
    languages?: string[];
    updateLocalStorageLanguages?: boolean;
    pageSize?: number;
  }) => {
    const [view, setView] = useUrlSearchState('view', { defaultVal: 'LIST' });
    const [initialLangs, setInitialLangs] = useState<
      string[] | null | undefined
    >(null);

    const languages = useApiQuery({
      url: '/v2/projects/{projectId}/languages',
      method: 'get',
      path: { projectId: props.projectId },
      query: { size: 1000, sort: ['tag'] },
      options: {
        onSuccess(data) {
          const requiredLanguages =
            props.languages ||
            projectPreferences.getForProject(props.projectId);
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
      keyName: props.keyName,
      pageSize: props.pageSize,
      updateLocalStorageLanguages: props.updateLocalStorageLanguages,
      // when initial langs are null, fetching is postponed
      initialLangs: initialLangs,
      baseLang: props.baseLang,
    });

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

    const dispatch = async (action: ActionType) => {
      switch (action.type) {
        case 'SET_SEARCH':
          translationService.setSearch(action.payload);
          return handleTranslationsReset();
        case 'SET_SEARCH_IMMEDIATE':
          translationService.setUrlSearch(action.payload);
          return handleTranslationsReset();
        case 'SET_FILTERS':
          translationService.setFilters(action.payload);
          return handleTranslationsReset();
        case 'SET_EDIT':
          return editService.setEdit(action.payload);
        case 'SET_EDIT_FORCE':
          return editService.setPositionAndFocus(action.payload);
        case 'UPDATE_EDIT':
          return editService.updatePosition(action.payload);
        case 'TOGGLE_SELECT':
          return selectionService.toggle(action.payload);
        case 'FETCH_MORE':
          return translationService.fetchNextPage();
        case 'CHANGE_FIELD':
          return editService.changeField(action.payload);
        case 'SELECT_LANGUAGES':
          translationService.updateQuery({ languages: action.payload });
          return handleTranslationsReset();
        case 'UPDATE_LANGUAGES':
          translationService.updateQuery({});
          return handleTranslationsReset();
        case 'UPDATE_SCREENSHOT_COUNT':
          return translationService.updateScreenshotCount(action.payload);
        case 'CHANGE_VIEW':
          return setView(action.payload);
        case 'DELETE_TRANSLATIONS':
          await selectionService.deleteSelected();
          return handleTranslationsReset();
        case 'SET_TRANSLATION_STATE':
          return stateService.changeState(action.payload);
        case 'ADD_TAG':
          return tagsService.addTag(action.payload);
        case 'REMOVE_TAG':
          return tagsService.removeTag(action.payload);
        case 'UPDATE_TRANSLATION':
          return translationService.updateTranslation(action.payload);
        case 'INSERT_TRANSLATION':
          return translationService.insertAsFirst(action.payload);
        case 'REGISTER_ELEMENT':
          return viewRefs.registerElement(action.payload);
        case 'UNREGISTER_ELEMENT':
          return viewRefs.unregisterElement(action.payload);
        case 'SCROLL_TO_ELEMENT':
          return viewRefs.scrollToElement(action.payload);
        case 'FOCUS_ELEMENT':
          return viewRefs.focusCell(action.payload);
        case 'REGISTER_LIST':
          return viewRefs.registerList(action.payload);
        case 'UNREGISTER_LIST':
          return viewRefs.unregisterList(action.payload);
      }
    };

    const dataReady = Boolean(
      languages.data && translationService.fixedTranslations
    );

    const state = {
      dataReady,
      // changes immediately when user clicks
      selectedLanguages: translationService.selectedLanguages,
      translations: dataReady
        ? translationService.fixedTranslations
        : undefined,
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
        selectionService.isLoading ||
        stateService.isLoading ||
        tagsService.isLoading,
      isEditLoading: editService.isLoading,
      isFetchingMore: translationService.isFetchingNextPage,
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

    return [state, dispatch];
  }
);
