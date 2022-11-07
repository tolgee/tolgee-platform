import React, { FunctionComponent } from 'react';
import AppBar from '@mui/material/AppBar';
import Dialog from '@mui/material/Dialog';
import IconButton from '@mui/material/IconButton';
import Slide from '@mui/material/Slide';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import { styled } from '@mui/material/styles';
import { TransitionProps } from '@mui/material/transitions';
import CloseIcon from '@mui/icons-material/Close';
import { T } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import { ImportConflictsData } from './ImportConflictsData';

const StyledAppBar = styled(AppBar)`
  position: relative;
`;

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
  return (
    <div>
      <Dialog
        fullScreen
        open={!!props.row}
        onClose={props.onClose}
        TransitionComponent={Transition}
        data-cy="import-conflict-resolution-dialog"
      >
        <StyledAppBar>
          <Toolbar>
            <IconButton
              edge="start"
              color="inherit"
              onClick={props.onClose}
              aria-label="close"
              data-cy="import-resolution-dialog-close-button"
              size="large"
            >
              <CloseIcon />
            </IconButton>
            <StyledTitle variant="h6">
              <T>import_resolve_conflicts_title</T>
            </StyledTitle>
          </Toolbar>
        </StyledAppBar>
        {!!props.row && <ImportConflictsData row={props.row} />}
      </Dialog>
    </div>
  );
};
