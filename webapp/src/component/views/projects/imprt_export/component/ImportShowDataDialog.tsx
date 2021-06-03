import React, { FunctionComponent, useState } from 'react';
import {
  createStyles,
  makeStyles,
  Theme,
  useTheme,
} from '@material-ui/core/styles';
import Dialog from '@material-ui/core/Dialog';
import AppBar from '@material-ui/core/AppBar';
import Toolbar from '@material-ui/core/Toolbar';
import IconButton from '@material-ui/core/IconButton';
import Typography from '@material-ui/core/Typography';
import CloseIcon from '@material-ui/icons/Close';
import Slide from '@material-ui/core/Slide';
import { TransitionProps } from '@material-ui/core/transitions';
import { components } from '../../../../../service/apiSchema';
import { T } from '@tolgee/react';
import { Box, Grid } from '@material-ui/core';
import { container } from 'tsyringe';
import { ImportActions } from '../../../../../store/project/ImportActions';
import { useProject } from '../../../../../hooks/useProject';
import { SimplePaginatedHateoasList } from '../../../../common/list/SimplePaginatedHateoasList';
import { SecondaryBar } from '../../../../layout/SecondaryBar';
import SearchField from '../../../../common/form/fields/SearchField';

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
                  languageId: props.row?.id!,
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
