import React, { ReactNode, useEffect, useState } from 'react';
import { useTranslate } from '@tolgee/react';
import { useQueryClient, UseQueryResult } from 'react-query';
import { container } from 'tsyringe';

import { FullPageLoading } from 'tg.component/common/FullPageLoading';
import { useProject } from 'tg.hooks/useProject';
import { useProjectLanguages } from 'tg.hooks/useProjectLanguages';
import { ProjectPreferencesService } from 'tg.service/ProjectPreferencesService';
import { components } from 'tg.service/apiSchema.generated';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { TranslationActions } from 'tg.store/project/TranslationActions';

import { useLeaveEditConfirmationOtherEdit } from './useLeaveEditConfirmation';

type TranslationsType =
  components['schemas']['ViewDataResponseLinkedHashSetKeyWithTranslationsResponseDtoResponseParams'];

export const TranslationListContext =
  // @ts-ignore
  React.createContext<TranslationListContextType>(null);

const actions = container.resolve(TranslationActions);
const selectedLanguagesService = container.resolve(ProjectPreferencesService);

export type TranslationListContextType = {
  listLanguages: string[];
  resetEdit: () => void;
  cellWidths: number[];
  headerCells: ReactNode[];
  refreshList: () => void;
  loadData: (search?: string, limit?: number, offset?: number) => void;
  listLoadable: UseQueryResult<TranslationsType>;
  perPage: number;
  checkAllToggle: () => void;
  isKeyChecked: (id: number) => boolean;
  toggleKeyChecked: (id: number) => void;
  isAllChecked: () => boolean;
  isSomeChecked: () => boolean;
  checkedKeys: Set<number>;
  showKeys: boolean;
  setShowKeys: (showKeys: boolean) => void;
  offset: number;
};

export const TranslationGridContextProvider: React.FC = ({ children }) => {
  const queryClient = useQueryClient();

  const projectDTO = useProject();
  const selectedLanguages = actions.useSelector((s) => s.selectedLanguages);

  const projectLanguages = useProjectLanguages().reduce(
    (acc, curr) => ({ ...acc, [curr.tag]: curr.name }),
    {}
  );

  const t = useTranslate();
  const [search, setSearch] = useState<string | undefined>();
  const [offset, setOffset] = useState<number | undefined>();
  const [perPage, setPerPage] = useState<number | undefined>();
  const [showKeys, setShowKeys] = useState(true);
  const [checkedKeys, setCheckedKeys] = useState(new Set<number>());
  const [_resetEdit, setResetEdit] = useState(() => () => {});

  const listLoadable = useApiQuery({
    url: '/api/project/{projectId}/translations/view',
    method: 'get',
    path: { projectId: projectDTO.id },
    query: {
      languages: selectedLanguages?.length ? selectedLanguages : undefined,
      search,
      limit: perPage || 20,
      offset: offset || 0,
    },
    options: {
      keepPreviousData: true,
      onSuccess: (data) => {
        // update selected languages in localstorage
        selectedLanguagesService.setForProject(
          projectDTO.id,
          data.params!.languages!
        );
      },
    },
  });

  const loadData = (search?: string, limit?: number, offset?: number) => {
    setSearch(search);
    setOffset(offset);
    setPerPage(limit);
    listLoadable.refetch();
  };

  const editLeaveConfirmation = useLeaveEditConfirmationOtherEdit();

  useEffect(() => {
    editLeaveConfirmation(
      () => {
        actions.otherEditionConfirm.dispatch();
      },
      () => {
        actions.otherEditionCancel.dispatch();
      }
    );
  });

  if (listLoadable.isLoading) {
    return <FullPageLoading />;
  }

  const isKeyChecked = (name) => checkedKeys.has(name);

  const isAllChecked = () => {
    return (
      listLoadable.data?.data?.filter((i) => !isKeyChecked(i.id)).length === 0
    );
  };

  const isSomeChecked = () => {
    return (
      Number(
        listLoadable.data?.data?.filter((i) => isKeyChecked(i.id)).length
      ) > 0
    );
  };

  const refreshList = () => {
    queryClient.invalidateQueries(['project', projectDTO.id, 'translations']);
  };

  // eslint-disable-next-line react/jsx-key
  const headerCells = showKeys ? [<b>{t('translation_grid_key_text')}</b>] : [];
  if (listLoadable.data?.params?.languages) {
    headerCells.push(
      ...listLoadable.data.params.languages.map((abbr, index) => (
        <b key={index}>{projectLanguages[abbr]}</b>
      ))
    );
  }

  const contextValue: TranslationListContextType = {
    checkAllToggle: () => {
      isAllChecked()
        ? setCheckedKeys(new Set())
        : setCheckedKeys(
            new Set<number>(
              listLoadable!.data!.data!.map((d) => d.id) as number[]
            )
          );
    },
    listLanguages: listLoadable.data?.params?.languages as string[],
    headerCells,
    cellWidths: headerCells.map((_) => 100 / headerCells.length),
    set resetEdit(resetEdit: () => void) {
      setResetEdit(() => resetEdit);
    },
    //set state accepts also a function, thats why the funcin returns function - to handle the react call
    get resetEdit() {
      return _resetEdit;
    },
    refreshList,
    loadData,
    listLoadable,
    perPage: perPage || 20,
    offset: offset || 0,
    isKeyChecked: isKeyChecked,
    toggleKeyChecked: (id) => {
      const copy = new Set<number>(checkedKeys);
      if (isKeyChecked(id)) {
        copy.delete(id);
      } else {
        copy.add(id);
      }
      setCheckedKeys(copy);
    },
    isAllChecked,
    isSomeChecked,
    checkedKeys: checkedKeys,
    showKeys,
    setShowKeys,
  };

  return (
    <TranslationListContext.Provider value={contextValue}>
      {children}
    </TranslationListContext.Provider>
  );
};
