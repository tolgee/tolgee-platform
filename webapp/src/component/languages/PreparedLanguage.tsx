import { Box, IconButton, styled } from '@mui/material';
import { Close, Edit } from '@mui/icons-material';

import { components } from 'tg.service/apiSchema.generated';

import { FlagImage } from './FlagImage';

const StyledContainer = styled('div')`
  border: 1px solid ${({ theme }) => theme.palette.emphasis['100']};
  padding: ${({ theme }) => theme.spacing(0.5, 1)};
  border-radius: ${({ theme }) => theme.shape.borderRadius};
  display: inline-flex;
  align-items: center;
`;

const StyledFlagImage = styled(FlagImage)`
  width: 20px;
  height: 20px;
  margin-right: ${({ theme }) => theme.spacing(1)};
`;

const StyledIconButton = styled(IconButton)`
  &.editButton {
    margin-left: ${({ theme }) => theme.spacing(1)};
  }
  & svg {
    width: 20px;
    height: 20px;
  }
`;

export const PreparedLanguage: React.FC<
  components['schemas']['LanguageDto'] & {
    onReset: () => void;
    onEdit: () => void;
  }
> = (props) => {
  return (
    <>
      <StyledContainer data-cy="languages-prepared-language-box">
        {props.flagEmoji && <StyledFlagImage flagEmoji={props.flagEmoji} />}{' '}
        <Box>
          {props.name} | {props.originalName} ({props.tag})
        </Box>
        <Box>
          <StyledIconButton
            data-cy="languages-create-customize-button"
            size="small"
            className="editButton"
            onClick={props.onEdit}
          >
            <Edit />
          </StyledIconButton>
          <StyledIconButton
            size="small"
            onClick={props.onReset}
            data-cy="languages-create-cancel-prepared-button"
          >
            <Close />
          </StyledIconButton>
        </Box>
      </StyledContainer>
    </>
  );
};
