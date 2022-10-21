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
import { container } from 'tsyringe';

import { paths } from '../apiSchema.generated';
import { paths as billingPaths } from '../billingApiSchema.generated';

import { RequestOptions } from './ApiHttpService';
import {
  ApiSchemaHttpService,
  RequestParamsType,
  ResponseContent,
} from './ApiSchemaHttpService';

const apiHttpService = container.resolve(ApiSchemaHttpService);

export type QueryProps<
  Url extends keyof Paths,
  Method extends keyof Paths[Url],
  Paths = paths
> = {
  url: Url;
  method: Method;
  fetchOptions?: RequestOptions;
  options?: UseQueryOptions<ResponseContent<Url, Method, Paths>>;
} & RequestParamsType<Url, Method, Paths>;

export type InfiniteQueryProps<
  Url extends keyof Paths,
  Method extends keyof Paths[Url],
  Paths = paths
> = {
  url: Url;
  method: Method;
  fetchOptions?: RequestOptions;
  options?: UseInfiniteQueryOptions<ResponseContent<Url, Method, Paths>>;
} & RequestParamsType<Url, Method, Paths>;

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
    any,
    RequestParamsType<Url, Method, Paths>
  >;
  invalidatePrefix?: string;
};

export const useApiInfiniteQuery = <
  Url extends keyof Paths,
  Method extends keyof Paths[Url],
  Paths = paths
>(
  props: InfiniteQueryProps<Url, Method, Paths>
) => {
  const { url, method, fetchOptions, options, ...request } = props;
  return useInfiniteQuery<ResponseContent<Url, Method, Paths>, any>(
    [url, (request as any)?.path, (request as any)?.query],
    ({ pageParam }) => {
      return apiHttpService.schemaRequest<Url, Method, Paths>(
        url,
        method,
        fetchOptions
      )(pageParam || request);
    },
    options
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

  return useQuery<ResponseContent<Url, Method, Paths>, any>(
    [url, (request as any)?.path, (request as any)?.query],
    ({ signal }) =>
      apiHttpService.schemaRequest<Url, Method, Paths>(url, method, {
        signal,
        ...fetchOptions,
      })(request),
    options
  );
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
  const mutation = useMutation<
    ResponseContent<Url, Method, Paths>,
    any,
    RequestParamsType<Url, Method, Paths>
  >(
    (request) =>
      apiHttpService.schemaRequest<Url, Method, Paths>(
        url,
        method,
        fetchOptions
      )(request),
    options
  );

  // inject custom onSuccess
  const customOptions = (options) => ({
    ...options,
    onSuccess: (...args) => {
      if (invalidatePrefix !== undefined) {
        invalidateUrlPrefix(queryClient, invalidatePrefix);
      }
      options?.onSuccess?.(...args);
    },
  });

  const mutate = useCallback<typeof mutation.mutate>(
    (variables, options) => {
      return mutation.mutate(variables, customOptions(options));
    },
    [mutation.mutate]
  );

  const mutateAsync = useCallback<typeof mutation.mutateAsync>(
    (variables, options) => {
      return mutation.mutateAsync(variables, customOptions(options));
    },
    [mutation.mutateAsync]
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

export const invalidateUrlPrefix = (queryClient: QueryClient, prefix: string) =>
  queryClient.invalidateQueries(matchUrlPrefix(prefix));

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
