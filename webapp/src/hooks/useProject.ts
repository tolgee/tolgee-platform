import { components } from '../service/apiSchema.generated';
import { useProjectContext } from './ProjectContext';

export const useProject = (): components['schemas']['ProjectModel'] => {
  const project = useProjectContext((c) => c.project);
  return project!;
};
