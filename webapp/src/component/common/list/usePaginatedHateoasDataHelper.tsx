import { ReactNode, useEffect, useState } from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { HateoasPaginatedData } from 'tg.service/response.types';
import {
  AbstractLoadableActions,
  StateWithLoadables,
} from 'tg.store/AbstractLoadableActions';

export type EmbeddedDataItem<
  ActionsType extends AbstractLoadableActions<StateWithLoadables<ActionsType>>,
  LoadableName extends keyof ActionsType['loadableDefinitions']
> = NonNullable<
  StateWithLoadables<ActionsType>['loadables'][LoadableName]['data']
> extends HateoasPaginatedData<infer ItemDataType>
  ? ItemDataType
  : never;

export const usePaginatedHateoasDataHelper = <
  ActionsType extends AbstractLoadableActions<StateWithLoadables<ActionsType>>,
  LoadableName extends keyof ActionsType['loadableDefinitions']
>(options: {
  actions: ActionsType;
  loadableName: LoadableName;
  dispatchParams?: [
    Omit<
      Parameters<
        ActionsType['loadableDefinitions'][LoadableName]['payloadProvider']
      >[0],
      'query'
    > & {
      query?: Omit<
        Parameters<
          ActionsType['loadableDefinitions'][LoadableName]['payloadProvider']
        >[0]['query'],
        'pageable'
      >;
    },
    ...any[]
  ];
  pageSize?: number;
  title?: ReactNode;
  search?: string;
  sortBy?: string[];
}) => {
  const loadable = options.actions.useSelector(
    (state) => state.loadables[options.loadableName]
  );
  const [currentPage, setCurrentPage] = useState(1);
  const [error, setError] = useState(false);

  const loadPage = (page: number) => {
    const [requestParam, ...otherParams] = options.dispatchParams
      ? [...options.dispatchParams]
      : [];

    const params = [
      {
        ...requestParam,
        query: {
          ...requestParam,
          pageable: {
            page: page - 1,
            size: options.pageSize || 20,
            sort: options.sortBy || ['name'],
          },
          search: options.search || undefined,
        },
      },
      ...otherParams,
    ] as Parameters<
      typeof options.actions.loadableActions[LoadableName]['dispatch']
    >;

    return options.actions.loadableActions[options.loadableName].dispatch(
      ...params
    );
  };

  useEffect(() => {
    if (options.search != undefined) {
      loadPage(1);
    }
  }, [options.search]);

  useEffect(() => {
    if (!loadable.touched) {
      loadPage(currentPage);
    }
  }, [loadable.touched]);

  useEffect(() => {
    return () => {
      (options.actions.loadableReset as any)[options.loadableName].dispatch();
    };
  }, []);

  const onPageChange = (page) => {
    setCurrentPage(page);
    loadPage(page);
  };

  useEffect(() => {
    if (loadable.error) {
      setError(true);
      // eslint-disable-next-line no-console
      console.error(error);
    }
  }, [loadable.error]);

  const data = loadable.data;
  const embedded = data?._embedded;
  const key = embedded ? Object.keys(embedded)?.[0] : null;
  const pageCount = data
    ? Math.ceil(data.page?.totalElements / data.page?.size)
    : undefined;
  const items = key ? embedded?.[key] : null;

  useEffect(() => {
    //move user to last page when pageCount is less then currentPage
    if (pageCount && pageCount < currentPage) {
      setCurrentPage(pageCount);
      loadPage(pageCount);
    }
  }, [loadable.data]);

  return {
    loading: loadable.loading,
    items,
    page: data?.page as components['schemas']['PageMetadata'],
    onPageChange,
    error,
  };
};
