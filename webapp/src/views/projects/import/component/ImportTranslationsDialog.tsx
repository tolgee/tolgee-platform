import React, { FunctionComponent, useState } from 'react';
import { Box, Grid, styled } from '@mui/material';
import Dialog from '@mui/material/Dialog';
import IconButton from '@mui/material/IconButton';
import Slide from '@mui/material/Slide';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import { useTheme } from '@mui/material/styles';
import { TransitionProps } from '@mui/material/transitions';
import { XClose } from '@untitled-ui/icons-react';
import { T } from '@tolgee/react';

import SearchField from 'tg.component/common/form/fields/SearchField';
import { SecondaryBar } from 'tg.component/layout/SecondaryBar';
import { useProject } from 'tg.hooks/useProject';
import { components } from 'tg.service/apiSchema.generated';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { StyledAppBar } from 'tg.component/layout/TopBar/TopBar';
import { TranslationVisual } from 'tg.views/projects/translations/translationVisual/TranslationVisual';

const StyledTitle = styled(Typography)`
  margin-left: ${({ theme }) => theme.spacing(2)};
  flex: 1;
`;

const StyledDescription = styled('div')`
  grid-area: description;
  font-size: 13px;
  color: ${({ theme }) =>
    theme.palette.mode === 'light'
      ? theme.palette.emphasis[300]
      : theme.palette.emphasis[500]};
`;

const Transition = React.forwardRef(function Transition(
  props: TransitionProps & { children: React.ReactElement },
  ref: React.Ref<unknown>
) {
  return <Slide direction="up" ref={ref} {...props} />;
});

export const ImportTranslationsDialog: FunctionComponent<{
  row?: components['schemas']['ImportLanguageModel'];
  onClose: () => void;
}> = (props) => {
  const project = useProject();
  const theme = useTheme();
  const [search, setSearch] = useState(undefined as string | undefined);
  const [page, setPage] = useState(0);

  const loadable = useApiQuery({
    url: '/v2/projects/{projectId}/import/result/languages/{languageId}/translations',
    method: 'get',
    path: {
      projectId: project.id,
      languageId: props.row?.id as any,
    },
    options: {
      enabled: !!props.row,
      keepPreviousData: true,
    },
    query: {
      onlyConflicts: false,
      page: page,
      size: 50,
      search: search,
    },
  });

  return (
    <div>
      <Dialog
        fullScreen
        open={!!props.row}
        onClose={props.onClose}
        TransitionComponent={Transition}
        data-cy="import-show-data-dialog"
        PaperProps={{ sx: { background: theme.palette.background.default } }}
      >
        <StyledAppBar sx={{ position: 'relative' }}>
          <Toolbar>
            <IconButton
              edge="start"
              color="inherit"
              onClick={props.onClose}
              aria-label="close"
              size="large"
            >
              <XClose />
            </IconButton>
            <StyledTitle variant="h6">
              <T keyName="import_show_translations_title" />
            </StyledTitle>
          </Toolbar>
        </StyledAppBar>
        <SecondaryBar>
          <SearchField onSearch={setSearch} />
        </SecondaryBar>
        {!!props.row && (
          <PaginatedHateoasList
            wrapperComponent={Box}
            wrapperComponentProps={{ sx: { m: 2 } }}
            onPageChange={setPage}
            loadable={loadable}
            renderItem={(i) => {
              return (
                <Box
                  pt={1}
                  pl={2}
                  pr={2}
                  style={{
                    borderBottom: `1px solid ${theme.palette.emphasis['100']}`,
                    wordBreak: 'break-all',
                  }}
                >
                  <Grid container spacing={2}>
                    <Grid item lg={4} md={3} sm xs>
                      <Box>{i.keyName}</Box>
                      {i.keyDescription && (
                        <StyledDescription>
                          {i.keyDescription}
                        </StyledDescription>
                      )}
                    </Grid>
                    <Grid item lg md sm xs>
                      <TranslationVisual
                        text={i.text}
                        locale={props.row?.existingLanguageTag ?? 'en'}
                        isPlural={i.isPlural}
                      />
                    </Grid>
                  </Grid>
                </Box>
              );
            }}
          />
        )}
      </Dialog>
    </div>
  );
};
