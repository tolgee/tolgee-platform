import React, { createContext } from 'react';
import { GlobalError } from '../error/GlobalError';
import { FullPageLoading } from '../component/common/FullPageLoading';
import { useGetProject, ProjectType } from '../service/hooks/Project';

export const ProjectContext = createContext<ProjectType | null>(null);

export const ProjectProvider: React.FC<{ id: number }> = ({ id, children }) => {
  const { isLoading, data, error } = useGetProject(id);

  if (isLoading) {
    return <FullPageLoading />;
  }

  if (data) {
    return (
      <ProjectContext.Provider value={data}>{children}</ProjectContext.Provider>
    );
  }

  throw new GlobalError(
    'Unexpected error occurred',
    error?.code || 'Loadable error'
  );
};
