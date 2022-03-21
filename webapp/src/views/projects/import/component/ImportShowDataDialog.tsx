import React, { FunctionComponent, useState } from 'react';
import { Box, Grid } from '@mui/material';
import AppBar from '@mui/material/AppBar';
import Dialog from '@mui/material/Dialog';
import IconButton from '@mui/material/IconButton';
import Slide from '@mui/material/Slide';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import { Theme, useTheme } from '@mui/material/styles';
import createStyles from '@mui/styles/createStyles';
import makeStyles from '@mui/styles/makeStyles';
import { TransitionProps } from '@mui/material/transitions';
import CloseIcon from '@mui/icons-material/Close';
import { T } from '@tolgee/react';
import { container } from 'tsyringe';

import SearchField from 'tg.component/common/form/fields/SearchField';
import { SimplePaginatedHateoasList } from 'tg.component/common/list/SimplePaginatedHateoasList';
import { SecondaryBar } from 'tg.component/layout/SecondaryBar';
import { useProject } from 'tg.hooks/useProject';
import { components } from 'tg.service/apiSchema.generated';
import { ImportActions } from 'tg.store/project/ImportActions';

const actions = container.resolve(ImportActions);
const useStyles = makeStyles<Theme>((theme) =>
  createStyles({
    appBar: {
      position: 'relative',
    },
    title: {
      marginLeft: theme.spacing(2),
      flex: 1,
    },
  })
);

const Transition = React.forwardRef(function Transition(
  props: TransitionProps & { children?: React.ReactElement },
  ref: React.Ref<unknown>
) {
  return <Slide direction="up" ref={ref} {...props} />;
});

export const ImportShowDataDialog: FunctionComponent<{
  row?: components['schemas']['ImportLanguageModel'];
  onClose: () => void;
}> = (props) => {
  const classes = useStyles();
  const project = useProject();
  const theme = useTheme();
  const [search, setSearch] = useState(undefined as string | undefined);

  return (
    <div>
      <Dialog
        fullScreen
        open={!!props.row}
        onClose={props.onClose}
        TransitionComponent={Transition}
        data-cy="import-show-data-dialog"
      >
        <AppBar className={classes.appBar}>
          <Toolbar>
            <IconButton
              edge="start"
              color="inherit"
              onClick={props.onClose}
              aria-label="close"
              size="large"
            >
              <CloseIcon />
            </IconButton>
            <Typography variant="h6" className={classes.title}>
              <T>import_show_translations_title</T>
            </Typography>
          </Toolbar>
        </AppBar>
        <SecondaryBar>
          <SearchField onSearch={setSearch} />
        </SecondaryBar>
        {!!props.row && (
          <SimplePaginatedHateoasList
            wrapperComponent={Box}
            wrapperComponentProps={{ m: 2 }}
            actions={actions}
            loadableName="translations"
            searchText={search}
            sortBy={[]}
            pageSize={50}
            dispatchParams={[
              {
                path: {
                  languageId: props.row?.id,
                  projectId: project.id,
                },
                query: {
                  onlyConflicts: false,
                },
              },
            ]}
            renderItem={(i) => (
              <Box
                pt={1}
                pl={2}
                pr={2}
                style={{
                  borderBottom: `1px solid ${theme.palette.grey['100']}`,
                  wordBreak: 'break-all',
                }}
              >
                <Grid container spacing={2}>
                  <Grid item lg={4} md={3} sm xs>
                    <Box>{i.keyName}</Box>
                  </Grid>
                  <Grid item lg md sm xs>
                    {i.text}
                  </Grid>
                </Grid>
              </Box>
            )}
          />
        )}
      </Dialog>
    </div>
  );
};
