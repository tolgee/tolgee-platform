import {
  QueryClient,
  useMutation,
  UseMutationOptions,
  useQuery,
  useQueryClient,
  UseQueryOptions,
} from 'react-query';
import { container } from 'tsyringe';
import { paths } from '../apiSchema.generated';
import { RequestOptions } from './ApiHttpService';
import {
  ApiSchemaHttpService,
  ResponseContent,
  RequestParamsType,
} from './ApiSchemaHttpService';

const apiHttpService = container.resolve(ApiSchemaHttpService);

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
  return useMutation<
    ResponseContent<Url, Method>,
    any,
    RequestParamsType<Url, Method>
  >(
    (request) =>
      apiHttpService.schemaRequest(url, method, fetchOptions)(request),
    {
      ...options,
      onSuccess(...args) {
        if (invalidatePrefix !== undefined) {
          invalidateUrlPrefix(queryClient, invalidatePrefix);
        }
        // call original onSuccess if present
        options?.onSuccess?.(...args);
      },
    }
  );
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
