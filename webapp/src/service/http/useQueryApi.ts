import { useCallback } from 'react';
import {
  Query,
  QueryClient,
  useInfiniteQuery,
  UseInfiniteQueryOptions,
  useMutation,
  UseMutationOptions,
  useQuery,
  useQueryClient,
  UseQueryOptions,
} from 'react-query';

import { paths } from '../apiSchema.generated';
import { paths as billingPaths } from '../billingApiSchema.generated';
import { ApiError } from './ApiError';

import { RequestOptions } from './ApiHttpService';
import {
  apiSchemaHttpService,
  RequestParamsType,
  ResponseContent,
} from './ApiSchemaHttpService';

export type QueryProps<
  Url extends keyof Paths,
  Method extends keyof Paths[Url],
  Paths = paths
> = {
  url: Url;
  method: Method;
  fetchOptions?: RequestOptions;
  options?: UseQueryOptions<ResponseContent<Url, Method, Paths>, ApiError>;
} & RequestParamsType<Url, Method, Paths>;

export type InfiniteQueryProps<
  Url extends keyof Paths,
  Method extends keyof Paths[Url],
  Paths = paths
> = {
  url: Url;
  method: Method;
  fetchOptions?: RequestOptions;
  options?: UseInfiniteQueryOptions<
    ResponseContent<Url, Method, Paths>,
    ApiError
  >;
} & RequestParamsType<Url, Method, Paths>;

type Split<S extends string> = S extends `${infer Prefix}/${infer Rest}`
  ? Prefix | `${Prefix}/${Split<Rest>}`
  : S;

// Create a union of all possible prefixes for all paths
type Prefix = Split<keyof (paths & billingPaths)> | '/';

export type MutationProps<
  Url extends keyof Paths,
  Method extends keyof Paths[Url],
  Paths = paths
> = {
  url: Url;
  method: Method;
  fetchOptions?: RequestOptions;
  options?: UseMutationOptions<
    ResponseContent<Url, Method, Paths>,
    ApiError,
    RequestParamsType<Url, Method, Paths>
  >;
  invalidatePrefix?: Prefix | Prefix[];
};

export const useApiInfiniteQuery = <
  Url extends keyof Paths,
  Method extends keyof Paths[Url],
  Paths = paths
>(
  props: InfiniteQueryProps<Url, Method, Paths>
) => {
  const { url, method, fetchOptions, options, ...request } = props;
  return useInfiniteQuery<ResponseContent<Url, Method, Paths>, ApiError>(
    [url, (request as any)?.path, (request as any)?.query],
    ({ pageParam }) => {
      return apiSchemaHttpService.schemaRequest<Url, Method, Paths>(
        url,
        method,
        {
          ...fetchOptions,
          disableAutoErrorHandle: true,
        }
      )(pageParam || request);
    },
    autoErrorHandling(options, Boolean(fetchOptions?.disableAutoErrorHandle))
  );
};

export const useApiQuery = <
  Url extends keyof Paths,
  Method extends keyof Paths[Url],
  Paths = paths
>(
  props: QueryProps<Url, Method, Paths>
) => {
  const { url, method, fetchOptions, options, ...request } = props;

  return useQuery<ResponseContent<Url, Method, Paths>, ApiError>(
    [url, (request as any)?.path, (request as any)?.query],
    ({ signal }) =>
      apiSchemaHttpService.schemaRequest<Url, Method, Paths>(url, method, {
        signal,
        ...fetchOptions,
        disableAutoErrorHandle: true,
      })(request),
    autoErrorHandling(
      options as UseQueryOptions<any, ApiError>,
      Boolean(fetchOptions?.disableAutoErrorHandle)
    )
  );
};

function autoErrorHandling(
  options: UseQueryOptions<any, ApiError> | undefined,
  disabled: boolean
) {
  return {
    ...options,
    onError: (err) => {
      if (options?.onError) {
        options.onError(err);
      } else if (err && !disabled) {
        (err as ApiError).handleError?.();
      }
    },
  };
}

function getApiMutationOptions(
  invalidatePrefix: undefined | string | string[],
  queryClient: QueryClient
) {
  return (options: UseQueryOptions<any, ApiError> | undefined) => ({
    ...options,
    onSuccess: (...params) => {
      // @ts-ignore
      options?.onSuccess?.(...params);
      if (invalidatePrefix !== undefined) {
        invalidateUrlPrefix(queryClient, invalidatePrefix);
      }
    },
  });
}

const getMutationCallback = <
  MutationFn extends (variables: any, options: any) => any
>(
  mutateFn: MutationFn,
  customOptions: ReturnType<typeof getApiMutationOptions>,
  fetchOptions: RequestOptions | undefined,
  props: any
) => {
  return useCallback<typeof mutateFn>(
    ((variables, options) => {
      return mutateFn(
        variables,
        autoErrorHandling(
          customOptions(options as any),
          Boolean(
            fetchOptions?.disableAutoErrorHandle || props.options?.onError
          )
        )
      );
    }) as any,
    [mutateFn]
  ) as MutationFn;
};
export const useApiMutation = <
  Url extends keyof Paths,
  Method extends keyof Paths[Url],
  Paths = paths
>(
  props: MutationProps<Url, Method, Paths>
) => {
  const queryClient = useQueryClient();
  const { url, method, fetchOptions, options, invalidatePrefix } = props;

  // inject custom onSuccess
  const customOptions = getApiMutationOptions(invalidatePrefix, queryClient);

  const mutation = useMutation<
    ResponseContent<Url, Method, Paths>,
    ApiError,
    RequestParamsType<Url, Method, Paths>
  >(
    (request) =>
      apiSchemaHttpService.schemaRequest<Url, Method, Paths>(url, method, {
        ...fetchOptions,
        disableAutoErrorHandle: true,
      })(request),
    customOptions(options as any) as any
  );

  const mutate = getMutationCallback(
    mutation.mutate,
    customOptions,
    fetchOptions,
    props
  );

  const mutateAsync = getMutationCallback(
    mutation.mutateAsync,
    customOptions,
    fetchOptions,
    props
  );

  return { ...mutation, mutate, mutateAsync };
};

export const matchUrlPrefix = (prefix: string) => {
  return {
    predicate: (query: Query) => {
      return (query.queryKey[0] as string)?.startsWith(prefix);
    },
  };
};

export const invalidateUrlPrefix = (
  queryClient: QueryClient,
  prefix: string | string[]
) => {
  if (typeof prefix === 'string') {
    queryClient.invalidateQueries(matchUrlPrefix(prefix));
  } else if (Array.isArray(prefix)) {
    prefix.forEach((p) => {
      queryClient.invalidateQueries(matchUrlPrefix(p));
    });
  }
};

export const useBillingApiQuery = <
  Url extends keyof billingPaths,
  Method extends keyof billingPaths[Url]
>(
  props: QueryProps<Url, Method, billingPaths>
) => useApiQuery<Url, Method, billingPaths>(props);

export const useBillingApiMutation = <
  Url extends keyof billingPaths,
  Method extends keyof billingPaths[Url]
>(
  props: MutationProps<Url, Method, billingPaths>
) => useApiMutation<Url, Method, billingPaths>(props);

export const useNdJsonStreamedMutation = <
  Url extends keyof Paths,
  Method extends keyof Paths[Url],
  Paths = paths
>(
  props: MutationProps<Url, Method, Paths> & { onData: (data: any) => void }
) => {
  const queryClient = useQueryClient();
  const { url, method, fetchOptions, options, onData, invalidatePrefix } =
    props;

  // inject custom onSuccess
  const customOptions = getApiMutationOptions(invalidatePrefix, queryClient);

  const mutation = useMutation<
    any[],
    ApiError,
    RequestParamsType<Url, Method, Paths>
  >(async (request) => {
    const response = await apiSchemaHttpService.schemaRequestRaw<
      Url,
      Method,
      Paths
    >(url, method, {
      ...fetchOptions,
    })(request);
    const reader = response.body?.getReader();
    const result: any[] = [];
    while (reader) {
      const { done, value } = await reader.read();
      const text = new TextDecoder().decode(value);
      if (text) {
        const parsed = getParsedJsonOrNull(text);
        if (!parsed) {
          continue;
        }
        if (parsed['error']) {
          const error = parsed['error'];
          throw new ApiError('Api error', error);
        }
        result.push(parsed);
        onData(parsed);
      }
      if (done) {
        break;
      }
    }
    return result;
  }, customOptions(options as any) as any);

  const mutate = getMutationCallback(
    mutation.mutate,
    customOptions,
    fetchOptions,
    props
  );

  const mutateAsync = getMutationCallback(
    mutation.mutateAsync,
    customOptions,
    fetchOptions,
    props
  );
  return { ...mutation, mutate, mutateAsync };
};

function getParsedJsonOrNull(json?: string): any {
  if (!json) {
    return null;
  }

  try {
    return JSON.parse(json);
  } catch (e) {
    return null;
  }
}
