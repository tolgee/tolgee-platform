import { Box } from '@material-ui/core';
import { useTranslate } from '@tolgee/react';
import { FunctionComponent, useContext } from 'react';
import { BoxLoading } from 'tg.component/common/BoxLoading';
import { FabAddButtonLink } from 'tg.component/common/buttons/FabAddButtonLink';
import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import { BaseView } from 'tg.component/layout/BaseView';
import { Navigation } from 'tg.component/navigation/Navigation';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { ProjectPermissionType } from 'tg.service/response.types';
import { Header } from './Header';
import { MenuBar } from './MenuBar';
import { Pagination } from './Pagination';
import { TranslationsRow } from './TranslationsRow';
import { TranslationListContext } from './TtranslationsGridContextProvider';

export const TranslationsGrid: FunctionComponent = (props) => {
  const projectDTO = useProject();

  const listContext = useContext(TranslationListContext);
  const isEmpty = listContext.listLoadable.data!.paginationMeta!.allCount === 0;
  const isSearch = Boolean(listContext.listLoadable.data!.params!.search);
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
      {listContext.listLoadable ? (
        <Box display="flex" flexDirection="column" flexGrow={1} fontSize={14}>
          <Header />
          {listContext.listLoadable!.data!.data!.map((t) => (
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
        (isSearch || !isEmpty) && (
          <>
            <Box mt={2}>
              <MenuBar />
            </Box>
          </>
        )
      }
      loading={listContext.listLoadable.isFetching}
      hideChildrenOnLoading={false}
    >
      {isEmpty ? onEmptyInner : onNotEmptyInner}
    </BaseView>
  );
};
