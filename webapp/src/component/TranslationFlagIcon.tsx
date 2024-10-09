import { Tooltip, styled } from '@mui/material';

export const StyledImgWrapper = styled('div')`
  display: flex;
  & svg {
    width: 15px;
    height: 15px;
  }
`;

export const StyledProviderImg = styled('img')`
  width: 14px;
  height: 14px;
`;

type Props = {
  icon: React.ReactNode;
  tooltip?: React.ReactNode;
};

const getContent = (props: { icon: React.ReactNode }) => {
  return <StyledImgWrapper>{props.icon}</StyledImgWrapper>;
};

export const TranslationFlagIcon: React.FC<Props> = ({ icon, tooltip }) => {
  return !tooltip ? (
    getContent({ icon })
  ) : (
    <Tooltip title={tooltip}>{getContent({ icon })}</Tooltip>
  );
};
