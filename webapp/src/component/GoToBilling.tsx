import { LINKS } from 'tg.constants/links';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';

type Props = {
  render: (props: {
    href: string;
    rel: string;
    target: string;
  }) => React.ReactElement;
};

export const GoToBilling = ({ render }: Props) => {
  const billingEnabled = useGlobalContext(
    (c) => c.initialData.serverConfiguration.billing.enabled
  );
  if (!billingEnabled) {
    return null;
  }
  return render({
    href: LINKS.GO_TO_CLOUD_BILLING.build(),
    rel: 'noopener noreferrer',
    target: '_blank',
  });
};
