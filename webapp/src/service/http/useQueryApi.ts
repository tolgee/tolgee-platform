import { useCallback } from 'react';
import {
  QueryClient,
  useInfiniteQuery,
  useMutation,
  UseMutationOptions,
  useQuery,
  useQueryClient,
  UseQueryOptions,
  UseInfiniteQueryOptions,
} from 'react-query';
import { container } from 'tsyringe';

import { paths } from '../apiSchema.generated';
import { RequestOptions } from './ApiHttpService';
import {
  ApiSchemaHttpService,
  RequestParamsType,
  ResponseContent,
} from './ApiSchemaHttpService';

const apiHttpService = container.resolve(ApiSchemaHttpService);

export const useApiInfiniteQuery = <
  Url extends keyof paths,
  Method extends keyof paths[Url]
>(
  props: {
    url: Url;
    method: Method;
    fetchOptions?: RequestOptions;
    options?: UseInfiniteQueryOptions<ResponseContent<Url, Method>>;
  } & RequestParamsType<Url, Method>
) => {
  const { url, method, fetchOptions, options, ...request } = props;
  return useInfiniteQuery<ResponseContent<Url, Method>, any>(
    [url, (request as any)?.path, (request as any)?.query],
    ({ pageParam }) => {
      return apiHttpService.schemaRequest(
        url,
        method,
        fetchOptions
      )(pageParam || request);
    },
    options
  );
};

export const useApiQuery = <
  Url extends keyof paths,
  Method extends keyof paths[Url]
>(
  props: {
    url: Url;
    method: Method;
    fetchOptions?: RequestOptions;
    options?: UseQueryOptions<ResponseContent<Url, Method>>;
  } & RequestParamsType<Url, Method>
) => {
  const { url, method, fetchOptions, options, ...request } = props;
  return useQuery<ResponseContent<Url, Method>, any>(
    [url, (request as any)?.path, (request as any)?.query],
    () => apiHttpService.schemaRequest(url, method, fetchOptions)(request),
    options
  );
};

export const useApiMutation = <
  Url extends keyof paths,
  Method extends keyof paths[Url]
>(props: {
  url: Url;
  method: Method;
  fetchOptions?: RequestOptions;
  options?: UseMutationOptions<
    ResponseContent<Url, Method>,
    any,
    RequestParamsType<Url, Method>
  >;
  invalidatePrefix?: string;
}) => {
  const queryClient = useQueryClient();
  const { url, method, fetchOptions, options, invalidatePrefix } = props;
  const mutation = useMutation<
    ResponseContent<Url, Method>,
    any,
    RequestParamsType<Url, Method>
  >(
    (request) =>
      apiHttpService.schemaRequest(url, method, fetchOptions)(request),
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

export const invalidateUrlPrefix = (
  queryClient: QueryClient,
  prefix: string
) => {
  queryClient.invalidateQueries({
    predicate: (query) => {
      return (query.queryKey[0] as string)?.startsWith(prefix);
    },
  });
};
