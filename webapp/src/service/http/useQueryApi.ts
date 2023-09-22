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
import { ApiError } from './ApiError';

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
  return useInfiniteQuery<ResponseContent<Url, Method, Paths>, ApiError>(
    [url, (request as any)?.path, (request as any)?.query],
    ({ pageParam }) => {
      return apiHttpService.schemaRequest<Url, Method, Paths>(url, method, {
        ...fetchOptions,
        disableAutoErrorHandle: true,
      })(pageParam || request);
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
      apiHttpService.schemaRequest<Url, Method, Paths>(url, method, {
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
  const customOptions = (
    options: UseQueryOptions<any, ApiError> | undefined
  ) => ({
    ...options,
    onSuccess: (...params) => {
      // @ts-ignore
      options?.onSuccess?.(...params);
      if (invalidatePrefix !== undefined) {
        invalidateUrlPrefix(queryClient, invalidatePrefix);
      }
    },
  });

  const mutation = useMutation<
    ResponseContent<Url, Method, Paths>,
    ApiError,
    RequestParamsType<Url, Method, Paths>
  >(
    (request) =>
      apiHttpService.schemaRequest<Url, Method, Paths>(url, method, {
        ...fetchOptions,
        disableAutoErrorHandle: true,
      })(request),
    customOptions(options as any) as any
  );

  const mutate = useCallback<typeof mutation.mutate>(
    (variables, options) => {
      return mutation.mutate(
        variables,
        autoErrorHandling(
          customOptions(options as any),
          Boolean(
            fetchOptions?.disableAutoErrorHandle || props.options?.onError
          )
        )
      );
    },
    [mutation.mutate]
  );

  const mutateAsync = useCallback<typeof mutation.mutateAsync>(
    (variables, options) => {
      return mutation.mutateAsync(
        variables,
        autoErrorHandling(
          customOptions(options as any),
          Boolean(
            fetchOptions?.disableAutoErrorHandle || props.options?.onError
          )
        )
      );
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
