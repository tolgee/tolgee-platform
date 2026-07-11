import { CheckCircleBroken } from '@untitled-ui/icons-react';
import { CheckCircleDash } from 'tg.component/CustomIcons';
import { components } from 'tg.service/apiSchema.generated';

type State = components['schemas']['TranslationViewModel']['state'];

type StateButtonProps = React.ComponentProps<typeof CheckCircleBroken> & {
  state: State | undefined;
};

export const StateIcon = ({ state, ...props }: StateButtonProps) => {
  switch (state) {
    case 'REVIEWED':
      return <CheckCircleDash {...props} />;
    default:
      return <CheckCircleBroken {...props} />;
  }
};
