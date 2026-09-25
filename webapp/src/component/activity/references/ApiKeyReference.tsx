import React from 'react';
import { ApiKeyReferenceData } from '../types';

type Props = {
  data: ApiKeyReferenceData;
};

export const ApiKeyReference: React.FC<React.PropsWithChildren<Props>> = ({
  data,
}) => {
  return <span className="reference referenceText">{data.description}</span>;
};
