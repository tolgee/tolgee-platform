import { FunctionComponent, useContext } from 'react';
import { useProject } from '../../hooks/useProject';
import { Box } from '@material-ui/core';
import { TranslationsRow } from './TranslationsRow';
import { Header } from './Header';
import { BoxLoading } from '../common/BoxLoading';
import { Pagination } from './Pagination';
import { LINKS, PARAMS } from '../../constants/links';
import { TranslationListContext } from './TtranslationsGridContextProvider';
import { EmptyListMessage } from '../common/EmptyListMessage';
import { FabAddButtonLink } from '../common/buttons/FabAddButtonLink';
import { MenuBar } from './MenuBar';
import { BaseView } from '../layout/BaseView';
import { useProjectPermissions } from '../../hooks/useProjectPermissions';
import { ProjectPermissionType } from '../../service/response.types';
import { T, useTranslate } from '@tolgee/react';

import { Navigation } from '../navigation/Navigation';
import Typography from '@material-ui/core/Typography';

export const TranslationsGrid: FunctionComponent = (props) => {
  let projectDTO = useProject();

  const listContext = useContext(TranslationListContext);
  const isEmpty = listContext.listLoadable.data!.paginationMeta.allCount === 0;
  const isSearch = listContext.listLoadable.data!.params.search;
  const projectPermissions = useProjectPermissions();

  const t = useTranslate();

  const onEmptyInner = (
    <>
      <EmptyListMessage />
      {!isSearch &&
        projectPermissions.satisfiesPermission(ProjectPermissionType.EDIT) && (
          <Box display="flex" justifyContent="center">
            <FabAddButtonLink
              to={LINKS.PROJECT_TRANSLATIONS_ADD.build({
                [PARAMS.PROJECT_ID]: projectDTO.id,
              })}
            />
          </Box>
        )}
    </>
  );

  const onNotEmptyInner = (
    <>
      {listContext.listLoadable.data ? (
        <Box display="flex" flexDirection="column" flexGrow={1} fontSize={14}>
          <Header />
          {listContext.listLoadable.data.data.map((t) => (
            <TranslationsRow key={t.name} data={t} />
          ))}
        </Box>
      ) : (
        <BoxLoading />
      )}
      <Box>
        <Pagination />
      </Box>
    </>
  );

  return (
    <BaseView
      navigation={
        <Navigation
          path={[
            [
              projectDTO.name,
              LINKS.PROJECT_TRANSLATIONS.build({
                [PARAMS.PROJECT_ID]: projectDTO.id,
              }),
            ],
            [
              t('translations_view_title'),
              LINKS.PROJECT_TRANSLATIONS.build({
                [PARAMS.PROJECT_ID]: projectDTO.id,
              }),
            ],
          ]}
        />
      }
      customHeader={
        isSearch ||
        (!isEmpty && (
          <>
            <Box mt={2}>
              <MenuBar />
            </Box>
          </>
        ))
      }
      loading={listContext.listLoadable.loading}
    >
      {isEmpty ? onEmptyInner : onNotEmptyInner}
    </BaseView>
  );
};
