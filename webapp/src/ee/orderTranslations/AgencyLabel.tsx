import { Box, styled } from '@mui/material';
import { components } from 'tg.service/apiSchema.generated';

type TranslationAgencySimpleModel =
  components['schemas']['TranslationAgencySimpleModel'];

const StyledAgencyName = styled(Box)`
  font-size: 16px;
  font-weight: 500;
`;

type Props = {
  agency: TranslationAgencySimpleModel;
};

export const AgencyLabel = ({ agency }: Props) => {
  return (
    <Box data-cy="agency-label" display="inline">
      {agency.avatar ? (
        <img src={agency.avatar?.thumbnail} alt={agency.name} width={75} />
      ) : (
        <StyledAgencyName style={{ margin: 0 }}>{agency.name}</StyledAgencyName>
      )}
    </Box>
  );
};
