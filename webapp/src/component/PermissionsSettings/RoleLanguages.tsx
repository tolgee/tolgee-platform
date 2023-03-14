import { styled, Box } from '@mui/material';
import { LanguagePermissionsMenu } from 'tg.component/security/LanguagePermissionsMenu';

import { LanguageModel, PermissionBasicState } from './types';

const StyledContainer = styled(Box)`
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 8px;
  & > * {
    margin-top: 8px;
  }
`;

type Props = {
  state: PermissionBasicState;
  onChange: (value: PermissionBasicState) => void;
  allLangs: LanguageModel[];
};

export const RoleLanguages: React.FC<Props> = ({
  state,
  onChange,
  allLangs,
}) => {
  const show = ['REVIEW', 'TRANSLATE'].includes(state.role!);

  const handleSelect = (values: number[]) => {
    onChange({
      ...state,
      languages: values,
    });
  };

  return show ? (
    <StyledContainer>
      <Box display="grid" gridAutoFlow="row" minWidth="200px">
        <LanguagePermissionsMenu
          selected={state.languages || []}
          onSelect={handleSelect}
          allLanguages={allLangs}
        />
      </Box>
    </StyledContainer>
  ) : null;
};
