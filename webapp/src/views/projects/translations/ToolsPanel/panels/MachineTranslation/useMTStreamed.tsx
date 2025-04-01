import { useQuery, UseQueryResult } from 'react-query';
import { useState } from 'react';

import { paths } from 'tg.service/apiSchema.generated';
import { ApiError } from 'tg.service/http/ApiError';
import { apiSchemaHttpService } from 'tg.service/http/ApiSchemaHttpService';
import { QueryProps } from 'tg.service/http/useQueryApi';

const URL =
  '/v2/projects/{projectId}/suggest/machine-translations-streaming' as const;
const METHOD = 'post';

type Url = typeof URL;
type Method = typeof METHOD;

type TypedData = QueryProps<Url, Method, paths>;

type ServiceOutput = {
  serviceType: string;
  result?: { output: string; contextDescription?: string };
  errorMessage: string | null;
  errorParams: string[] | undefined;
};

export type CombinedMTResponse = {
  servicesTypes: string[];
  result: Record<string, ServiceOutput | undefined>;
  baseBlank?: boolean;
};

function combineChunks(meshedChunks: string): CombinedMTResponse {
  const messages = meshedChunks
    .split('\n')
    .filter((item) => Boolean(item))
    .map((data) => {
      return JSON.parse(data);
    });
  const result: CombinedMTResponse = {
    servicesTypes: [],
    result: {},
    baseBlank: false,
  };
  messages.forEach((message) => {
    if (message.servicesTypes) {
      result.servicesTypes = message.servicesTypes;
      result.baseBlank = message.baseBlank;
    }
    if (message.serviceType) {
      result.result[message.serviceType] = message;
    }
  });
  return result;
}

export const useMTStreamed = (props: Omit<TypedData, 'url' | 'method'>) => {
  const { fetchOptions, options, ...request } = props;
  const queryKey = [URL, (request as any)?.path, (request as any)?.query];
  const [preData, setPreData] = useState<CombinedMTResponse>();

  const queryLoadable = useQuery<CombinedMTResponse, ApiError>(
    queryKey,
    async ({ signal }) => {
      const response = await apiSchemaHttpService.schemaRequestRaw<
        Url,
        Method,
        paths
      >(URL, METHOD, {
        signal,
        ...fetchOptions,
        disableAutoErrorHandle: true,
      })(request);
      const reader = response.body?.getReader();
      let combinedText = '';
      while (reader) {
        const { done, value } = await reader.read();
        const text = new TextDecoder().decode(value);
        if (text) {
          combinedText = combinedText + text;
        }
        const streamedResult = combineChunks(combinedText);
        setPreData(streamedResult);
        if (done) {
          // Do something with last chunk of data then exit reader
          break;
        }
      }
      return combineChunks(combinedText);
    },
    {
      ...(options as any),
      onSettled(data, error) {
        setPreData(undefined);
        options?.onSettled?.(data as any, error);
      },
    }
  );

  return {
    ...queryLoadable,
    data: queryLoadable.data ?? preData,
  } as UseQueryResult<CombinedMTResponse, ApiError>;
};
