import {
  MutationProps,
  QueryProps,
  useApiMutation,
  useApiQuery,
} from 'tg.service/http/useQueryApi';

import { paths } from 'tg.service/billingApiSchema.generated';

export const useBillingApiQuery = <
  Url extends keyof paths,
  Method extends keyof paths[Url]
>(
  props: QueryProps<Url, Method, paths>
) => useApiQuery<Url, Method, paths>(props);

export const useBillingApiMutation = <
  Url extends keyof paths,
  Method extends keyof paths[Url]
>(
  props: MutationProps<Url, Method, paths>
) => useApiMutation<Url, Method, paths>(props);
