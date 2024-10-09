import React, { FunctionComponent } from 'react';
import Dialog from '@mui/material/Dialog';
import IconButton from '@mui/material/IconButton';
import Slide from '@mui/material/Slide';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import { styled } from '@mui/material/styles';
import { TransitionProps } from '@mui/material/transitions';
import { XClose } from '@untitled-ui/icons-react';
import { T } from '@tolgee/react';
import { useTheme } from '@mui/material';

import { components } from 'tg.service/apiSchema.generated';
import { ImportConflictsData } from './ImportConflictsData';
import { StyledAppBar } from 'tg.component/layout/TopBar/TopBar';

const StyledTitle = styled(Typography)`
  margin-left: ${({ theme }) => theme.spacing(2)};
  flex: 1;
`;

const Transition = React.forwardRef(function Transition(
  props: TransitionProps & { children: React.ReactElement },
  ref: React.Ref<unknown>
) {
  return <Slide direction="up" ref={ref} {...props} />;
});

export const ImportConflictResolutionDialog: FunctionComponent<{
  row?: components['schemas']['ImportLanguageModel'];
  onClose: () => void;
}> = (props) => {
  const theme = useTheme();
  return (
    <div>
      <Dialog
        fullScreen
        open={!!props.row}
        onClose={props.onClose}
        TransitionComponent={Transition}
        data-cy="import-conflict-resolution-dialog"
        PaperProps={{ sx: { background: theme.palette.background.default } }}
      >
        <StyledAppBar sx={{ position: 'relative' }}>
          <Toolbar>
            <IconButton
              edge="start"
              color="inherit"
              onClick={props.onClose}
              aria-label="close"
              data-cy="import-resolution-dialog-close-button"
              size="large"
            >
              <XClose />
            </IconButton>
            <StyledTitle variant="h6">
              <T keyName="import_resolve_conflicts_title" />
            </StyledTitle>
          </Toolbar>
        </StyledAppBar>
        {!!props.row && <ImportConflictsData row={props.row} />}
      </Dialog>
    </div>
  );
};
