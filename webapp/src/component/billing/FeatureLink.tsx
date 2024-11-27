import { StyledBillingLink } from 'tg.ee/billing/component/Decorations';

type Props = {
  href: string;
  newTab?: boolean;
};

export const FeatureLink: React.FC<Props> = ({ children, href, newTab }) => {
  if (newTab) {
    return (
      <StyledBillingLink href={href} target="_blank" rel="noreferrer noopener">
        {children}
      </StyledBillingLink>
    );
  } else {
    return <StyledBillingLink href={href}>{children}</StyledBillingLink>;
  }
};
