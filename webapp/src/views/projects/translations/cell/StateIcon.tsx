import { CheckCircleOutlined, CheckCircle } from '@mui/icons-material';
import { components } from 'tg.service/apiSchema.generated';

type State = components['schemas']['TranslationViewModel']['state'];

type StateButtonProps = React.ComponentProps<typeof CheckCircleOutlined> & {
  state: State | undefined;
};

export const StateIcon = ({ state, ...props }: StateButtonProps) => {
  switch (state) {
    case 'REVIEWED':
      return <CheckCircle {...props} />;
    default:
      return <CheckCircleOutlined {...props} />;
  }
};
