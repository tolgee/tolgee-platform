import { useTranslate } from '@tolgee/react';
import { Share04 } from '@untitled-ui/icons-react';
import { Box, Link, SxProps, css, styled } from '@mui/material';

const buttonStyle = css`
  text-decoration-line: underline;
  display: flex;
  justify-self: center;
  gap: 1px;
  font-size: 13px;
  cursor: pointer;
`;

const StyledLink = styled(Link)`
  ${buttonStyle}
  color: ${({ theme }) => theme.palette.text.secondary};
`;

const StyledButton = styled(Box)`
  ${buttonStyle}
  color: ${({ theme }) => theme.palette.text.secondary};
`;

const StyledIcon = styled(Share04)`
  width: 15px;
  height: 15px;
  position: relative;
  top: 2px;
`;

type LinkProps = {
  sx?: SxProps;
  className?: string;
};

export const ShowAllFeaturesLink = ({ sx, className }: LinkProps) => {
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

type ButtonProps = {
  sx?: SxProps;
  className?: string;
  onClick?: () => void;
};

export const ShowAllFeaturesButton = ({
  sx,
  className,
  onClick,
}: ButtonProps) => {
  const { t } = useTranslate();
  return (
    <StyledButton onClick={() => onClick?.()} {...{ sx, className }}>
      {t('billing_subscriptions_show_all_features')}
    </StyledButton>
  );
};
