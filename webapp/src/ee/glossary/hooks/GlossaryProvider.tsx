import { components } from 'tg.service/apiSchema.generated';
import React, { useMemo } from 'react';

type GlossaryModel = components['schemas']['GlossaryModel'];
type SimpleProjectModel = components['schemas']['SimpleProjectModel'];

type Props = {
  glossary: GlossaryModel;
  assignedProjects: SimpleProjectModel[];
};

type AssignedProjects = {
  assignedProjects: components['schemas']['SimpleProjectModel'][];
};

type ContextData = {
  glossary: components['schemas']['GlossaryModel'] & AssignedProjects;
};

export const GlossaryContextHolder = React.createContext<ContextData>(
  null as any
);

export const GlossaryProvider: React.FC<Props> = ({
  children,
  glossary,
  assignedProjects,
}) => {
  const result: ContextData = useMemo(() => {
    return {
      glossary: {
        ...glossary,
        assignedProjects: assignedProjects,
      },
    };
  }, [glossary, assignedProjects]);

  return (
    <GlossaryContextHolder.Provider value={result as ContextData}>
      {children}
    </GlossaryContextHolder.Provider>
  );
};
