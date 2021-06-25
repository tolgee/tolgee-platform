import React, { FunctionComponent, useState } from 'react';
import { Box, Grid } from '@material-ui/core';
import AppBar from '@material-ui/core/AppBar';
import Dialog from '@material-ui/core/Dialog';
import IconButton from '@material-ui/core/IconButton';
import Slide from '@material-ui/core/Slide';
import Toolbar from '@material-ui/core/Toolbar';
import Typography from '@material-ui/core/Typography';
import {
  createStyles,
  makeStyles,
  Theme,
  useTheme,
} from '@material-ui/core/styles';
import { TransitionProps } from '@material-ui/core/transitions';
import CloseIcon from '@material-ui/icons/Close';
import { T } from '@tolgee/react';
import { container } from 'tsyringe';

import SearchField from 'tg.component/common/form/fields/SearchField';
import { SimplePaginatedHateoasList } from 'tg.component/common/list/SimplePaginatedHateoasList';
import { SecondaryBar } from 'tg.component/layout/SecondaryBar';
import { useProject } from 'tg.hooks/useProject';
import { components } from 'tg.service/apiSchema.generated';
import { ImportActions } from 'tg.store/project/ImportActions';

const actions = container.resolve(ImportActions);
const useStyles = makeStyles((theme: Theme) =>
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
