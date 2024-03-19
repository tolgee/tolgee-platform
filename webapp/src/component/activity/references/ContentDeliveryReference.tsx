import React from 'react';
import { ContentDeliveryConfigReferenceData } from '../types';

type Props = {
  data: ContentDeliveryConfigReferenceData;
};

export const ContentDeliveryReference: React.FC<Props> = ({ data }) => {
  return <span className="reference referenceText">{data.name}</span>;
};
