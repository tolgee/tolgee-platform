import React from 'react';
import { ContentStorageReferenceData } from '../types';

type Props = {
  data: ContentStorageReferenceData;
};

export const ContentStorageReference: React.FC<
  React.PropsWithChildren<Props>
> = ({ data }) => {
  return <span className="reference referenceText">{data.name}</span>;
};
