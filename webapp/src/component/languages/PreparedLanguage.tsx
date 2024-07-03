import { Box, IconButton, styled } from '@mui/material';
import { XClose, Edit02 } from '@untitled-ui/icons-react';

import { components } from 'tg.service/apiSchema.generated';

import { FlagImage } from './FlagImage';

const StyledContainer = styled('div')`
  background: ${({ theme }) => theme.palette.languageChips.background};
  padding: ${({ theme }) => theme.spacing(0.5, 0.5, 0.5, 1.5)};
  border-radius: ${({ theme }) => theme.shape.borderRadius}px;
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
  components['schemas']['LanguageRequest'] & {
    onReset: () => void;
    onEdit: () => void;
  }
> = (props) => {
  return (
    <>
      <StyledContainer data-cy="languages-prepared-language-box">
        {props.flagEmoji && <StyledFlagImage flagEmoji={props.flagEmoji} />}{' '}
        <Box display="flex" alignItems="center">
          {props.name} | {props.originalName} ({props.tag})
        </Box>
        <Box display="flex" alignItems="center">
          <StyledIconButton
            data-cy="languages-create-customize-button"
            size="small"
            className="editButton"
            onClick={props.onEdit}
          >
            <Edit02 />
          </StyledIconButton>
          <StyledIconButton
            size="small"
            onClick={props.onReset}
            data-cy="languages-create-cancel-prepared-button"
          >
            <XClose />
          </StyledIconButton>
        </Box>
      </StyledContainer>
    </>
  );
};
