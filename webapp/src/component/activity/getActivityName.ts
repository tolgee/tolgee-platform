import { components } from 'tg.service/apiSchema.generated';
import { actionsConfiguration } from './configuration';

type ProjectActivityModel = components['schemas']['ProjectActivityModel'];

type Props = {
  data: ProjectActivityModel;
};

export const getActivityLabel = ({ data }: Props) => {
  const config = actionsConfiguration[data.type];
  return config
    ? config.label + ' ' + (config.labelDescription?.(data) || '')
    : data.type;
};
