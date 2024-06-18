import { useTranslate } from '@tolgee/react';
import { OpenInNew } from '@mui/icons-material';
import { Link, SxProps, styled } from '@mui/material';

const StyledLink = styled(Link)`
  text-decoration-line: underline;
  color: ${({ theme }) => theme.palette.text.secondary};
  display: flex;
  justify-self: center;
  gap: 1px;
  font-size: 13px;
`;

const StyledIcon = styled(OpenInNew)`
  font-size: 15px;
  position: relative;
  top: 2px;
`;

type Props = {
  sx?: SxProps;
  className?: string;
};

export const ShowAllFeaturesLink = ({ sx, className }: Props) => {
  const { t } = useTranslate();
  return (
    <StyledLink
      href="https://tolgee.io/pricing#features-table"
      target="_blank"
      rel="noreferrer noopener"
      {...{ sx, className }}
    >
      {t('billing_subscriptions_show_all_features')}
      <StyledIcon />
    </StyledLink>
  );
};
