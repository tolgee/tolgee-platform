import { CheckCircleOutlined, ErrorOutlined } from '@material-ui/icons';

import { StateType } from 'tg.constants/translationStates';

type StateButtonProps = React.ComponentProps<typeof CheckCircleOutlined> & {
  state: StateType;
};

export const StateIcon = ({ state, ...props }: StateButtonProps) => {
  switch (state) {
    case 'NEEDS_REVIEW':
      return <ErrorOutlined {...props} />;
    default:
      return <CheckCircleOutlined {...props} />;
  }
};
