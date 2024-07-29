import { styled } from '@mui/material';
import React from 'react';

const StyledContainer = styled('div')`
  border-radius: 8px;
  background: ${({ theme }) => theme.palette.background.default};
  box-shadow: 0px 0px 4px 0px rgba(0, 0, 0, 0.25);
  padding: 30px 20px 20px 20px;
  min-width: 200px;
  display: grid;
  justify-items: center;
  position: relative;
`;

const StyledPlatform = styled('div')`
  position: absolute;
  top: 8px;
  left: 8px;
`;

const StyledImage = styled('div')`
  padding-bottom: 6px;
  display: flex;
  align-items: center;
  height: 48px;
`;

const StyledName = styled('div')`
  font-size: 16px;
  color: ${({ theme }) => theme.palette.text.primary};
`;

const StyledDescription = styled('div')`
  color: ${({ theme }) => theme.palette.text.secondary};
  font-size: 12px;
  font-weight: 400;
`;

export type UserInfo = {
  platformImage: React.ReactNode;
  image: React.ReactNode;
  name: string | undefined;
  description: string | undefined;
};

type Props = {
  data: UserInfo;
};

export const ConnectionItem = ({ data }: Props) => {
  const { platformImage, image, name, description } = data;
  return (
    <StyledContainer>
      <StyledPlatform>{platformImage}</StyledPlatform>
      <StyledImage>{image}</StyledImage>
      <StyledName>{name}</StyledName>
      <StyledDescription>{description}</StyledDescription>
    </StyledContainer>
  );
};
