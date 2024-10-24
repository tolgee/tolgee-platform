import { Box, styled } from '@mui/material';
import { ProviderDescription } from './ProviderDescription';
import clsx from 'clsx';
import { LinkExternal01 } from '@untitled-ui/icons-react';
import { isValidHttpUrl } from 'tg.fixtures/isValidUrl';
import { components } from 'tg.service/billingApiSchema.generated';

type TranslationAgencyModel = components['schemas']['TranslationAgencyModel'];

const StyledLinkExternal01 = styled(LinkExternal01)`
  margin-left: 3px;
  position: relative;
  top: 2px;
`;

const StyledContainer = styled(Box)`
  border: 1px solid ${({ theme }) => theme.palette.tokens.border.soft};
  gap: 20px;
  display: grid;
  border-radius: 16px;
  background: ${({ theme }) => theme.palette.tokens.background['paper-2']};
  padding: 20px;
  cursor: pointer;
  transition: box-shadow ease-in-out 0.2s, border-color ease-in-out 0.2s;
  &.selected {
    border-color: ${({ theme }) => theme.palette.primary.main};
    cursor: unset;
    box-shadow: 0px 0px 17px 0px
      ${({ theme }) => theme.palette.primary.main + '55'};
  }
`;

const StyledServices = styled(Box)`
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
`;

const StyledDescription = styled(Box)`
  background: ${({ theme }) => theme.palette.tokens.background['paper-3']};
  padding: 0px 16px;
  border-radius: 16px;
`;

type Props = {
  agency: TranslationAgencyModel;
  selected: boolean;
  onSelect: (id: number) => void;
};

export const TranslationAgency = ({ agency, selected, onSelect }: Props) => {
  const url =
    agency.url && isValidHttpUrl(agency.url) ? new URL(agency.url) : undefined;
  return (
    <StyledContainer
      className={clsx({ selected })}
      onClick={() => onSelect(agency.id)}
    >
      <Box
        display="flex"
        justifyContent="space-between"
        flexWrap="wrap"
        alignItems="start"
      >
        {agency.avatar ? (
          <img src={agency.avatar?.large} alt={agency.name} width={150} />
        ) : (
          <h2 style={{ margin: 0 }}>{agency.name}</h2>
        )}
        <Box display="flex" alignItems="center" gap="20px">
          {url && (
            <a target="_blank" href={url.toString()} rel="noreferrer">
              {url.host}
              <StyledLinkExternal01 width={14} height={14} />
            </a>
          )}
        </Box>
      </Box>
      {Boolean(agency.services.length) && (
        <StyledServices>
          {agency.services.map((item) => (
            <Box key={item}>{item}</Box>
          ))}
        </StyledServices>
      )}
      {agency.description && (
        <StyledDescription>
          <ProviderDescription description={agency.description} />
        </StyledDescription>
      )}
    </StyledContainer>
  );
};
