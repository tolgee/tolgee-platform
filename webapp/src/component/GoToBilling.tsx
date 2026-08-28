import { LINKS } from 'tg.constants/links';
import { useBillingRefusal } from 'tg.globalContext/helpers';

type Props = {
  render: (props: {
    href: string;
    rel: string;
    target: string;
  }) => React.ReactElement;
  /** Rendered for a viewer who has an organization but may not act on its billing. */
  fallback?: React.ReactNode;
};

export const GoToBilling = ({ render, fallback }: Props) => {
  const refusal = useBillingRefusal();

  if (refusal === 'billing-not-an-owner') {
    return <>{fallback ?? null}</>;
  }

  // Anything else — billing off, or no organization at all — has nothing to say to this viewer.
  if (refusal) {
    return null;
  }
  return render({
    href: LINKS.GO_TO_CLOUD_BILLING.build(),
    rel: 'noopener noreferrer',
    target: '_blank',
  });
};
