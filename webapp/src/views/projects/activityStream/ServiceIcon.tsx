import { Tooltip } from '@mui/material';
import { useServiceImg } from 'tg.hooks/useServiceImg';
import { useServiceName } from 'tg.hooks/useServiceName';

type Props = {
  provider: string | undefined | null;
};

export const ServiceIcon = ({ provider }: Props) => {
  const getServiceImg = useServiceImg();
  const getServiceName = useServiceName();
  const img = provider ? getServiceImg(provider, false) : undefined;
  if (!img) return null;
  return (
    <Tooltip title={getServiceName(provider as never)} disableInteractive>
      <img
        src={img}
        alt={provider ?? ''}
        height={14}
        style={{ verticalAlign: 'middle' }}
        data-cy="activity-service-icon"
      />
    </Tooltip>
  );
};
