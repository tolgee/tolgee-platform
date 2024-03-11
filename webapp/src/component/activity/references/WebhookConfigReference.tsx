import React from 'react';
import { WebhookConfigReferenceData } from '../types';

type Props = {
  data: WebhookConfigReferenceData;
};

export const WebhookConfigReference: React.FC<Props> = ({ data }) => {
  return <span className="reference referenceText">{data.url}</span>;
};
