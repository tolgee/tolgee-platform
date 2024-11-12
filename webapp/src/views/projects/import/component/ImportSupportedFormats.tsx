import { Box, styled, Typography } from '@mui/material';
import { T } from '@tolgee/react';
import React from 'react';
import TolgeeLogo from 'tg.svgs/tolgeeLogo.svg?react';
import IcuLogo from 'tg.svgs/logos/icu.svg?react';
import PhoLogo from 'tg.svgs/logos/php.svg?react';
import CLogo from 'tg.svgs/logos/c.svg?react';
import PythonLogo from 'tg.svgs/logos/python.svg?react';
import AppleLogo from 'tg.svgs/logos/apple.svg?react';
import AndroidLogo from 'tg.svgs/logos/android.svg?react';
import FluttrerLogo from 'tg.svgs/logos/flutter.svg?react';
import RailsLogo from 'tg.svgs/logos/rails.svg?react';
import I18nextLogo from 'tg.svgs/logos/i18next.svg?react';
import CsvLogo from 'tg.svgs/logos/csv.svg?react';

const TechLogo = ({
  svg,
  height,
  width,
}: {
  svg: React.ReactNode;
  height?: string;
  width?: string;
}) => {
  return (
    <Box
      sx={(theme) => ({
        color: theme.palette.tokens.text.secondary,
        height: height || '20px',
        width,
      })}
    >
      {svg}
    </Box>
  );
};

const FORMATS = [
  {
    name: 'JSON',
    logo: <TolgeeLogo />,
    logoHeight: '24px',
    logoWidth: '24px',
  },
  {
    name: 'XLIFF',
    logo: <IcuLogo />,
  },
  { name: 'PO PHP', logo: <PhoLogo /> },
  { name: 'PO C/C++', logo: <CLogo /> },
  { name: 'PO Python', logo: <PythonLogo /> },
  { name: 'Apple Strings', logo: <AppleLogo /> },
  { name: 'Apple Stringsdict', logo: <AppleLogo /> },
  { name: 'Apple XLIFF', logo: <AppleLogo /> },
  { name: 'Android XML', logo: <AndroidLogo /> },
  { name: 'Flutter ARB', logo: <FluttrerLogo /> },
  { name: 'Ruby YAML', logo: <RailsLogo /> },
  { name: 'i18next', logo: <I18nextLogo /> },
  { name: 'CSV', logo: <CsvLogo /> },
];

export const ImportSupportedFormats = () => {
  return (
    <>
      <Typography
        variant="body1"
        sx={(theme) => ({
          color: theme.palette.tokens.text.secondary,
          marginBottom: '16px',
          marginTop: '16px',
          textAlign: 'center',
        })}
      >
        <T keyName="import_file_supported_formats_title" />
      </Typography>
      <StyledContainer>
        {FORMATS.map((f) => (
          <Item
            key={f.name}
            name={f.name}
            logo={f.logo}
            logoHeight={f.logoHeight}
            logoWidth={f.logoWidth}
          />
        ))}
      </StyledContainer>
    </>
  );
};

const Item = ({
  name,
  logo,
  logoHeight,
  logoWidth,
}: {
  name: string;
  logo?: React.ReactNode;
  logoHeight?: string;
  logoWidth?: string;
}) => {
  return (
    <StyledItem>
      <TechLogo svg={logo} height={logoHeight} width={logoWidth} />
      {name}
    </StyledItem>
  );
};

const StyledItem = styled('div')`
  height: 36px;
  display: inline-flex;
  padding: 8px 12px;
  justify-content: center;
  align-items: center;
  gap: 4px;
  border-radius: 12px;
  border: 1px solid ${({ theme }) => theme.palette.tokens.border.secondary};
  color: ${({ theme }) => theme.palette.tokens.text.secondary};
  background-color: ${({ theme }) =>
    theme.palette.tokens.background['paper-2']};
  font-size: 15px;
`;

const StyledContainer = styled('div')`
  display: flex;
  max-width: 795px;
  justify-content: center;
  align-items: center;
  align-content: center;
  gap: 8px;
  flex-shrink: 0;
  flex-wrap: wrap;
`;
