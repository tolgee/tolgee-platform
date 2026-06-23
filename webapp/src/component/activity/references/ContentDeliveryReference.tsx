import React from 'react';
import { ContentDeliveryConfigReferenceData } from '../types';

type Props = {
  data: ContentDeliveryConfigReferenceData;
};

export const ContentDeliveryReference: React.FC<
  React.PropsWithChildren<Props>
> = ({ data }) => {
  return <span className="reference referenceText">{data.name}</span>;
};
