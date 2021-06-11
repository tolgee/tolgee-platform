import { useQuery } from 'react-query';
import { container } from 'tsyringe';

import { ApiV2HttpService } from '../http/ApiV2HttpService';
import { components } from '../apiSchema.generated';

const httpV2 = container.resolve(ApiV2HttpService);

export type ProjectType = components['schemas']['ProjectModel'];

export const useGetProject = (id: number) => {
  return useQuery<ProjectType, any>(['project', id], () =>
    httpV2.get('projects/' + id)
  );
};
