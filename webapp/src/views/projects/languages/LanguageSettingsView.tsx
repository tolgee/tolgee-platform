import { useTranslate } from '@tolgee/react';
import { ProjectLanguages } from 'tg.views/projects/languages/ProjectLanguages';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';
import { Box, Tab, Tabs, styled } from '@mui/material';
import { Link, useRouteMatch } from 'react-router-dom';

import { BaseProjectView } from '../BaseProjectView';
import { MachineTranslation } from './MachineTranslation/MachineTranslation';
import { LanguageEditDialog } from './LanguageEdit/LanguageEditDialog';
import { QuickStartHighlightInPortal } from 'tg.component/layout/QuickStartGuide/QuickStartHighlightInPortal';

const StyledTabs = styled(Tabs)`
  margin-bottom: -1px;
  overflow: visible;
  & * {
    overflow: visible;
  }
`;

const StyledTabWrapper = styled(Box)`
  border-bottom: 1px solid ${({ theme }) => theme.palette.divider1};
`;

export const LanguageSettingsView = () => {
  const { t } = useTranslate();

  const pageLanguages = useRouteMatch(LINKS.PROJECT_LANGUAGES.template);
  const pageEditLanguage = useRouteMatch(LINKS.PROJECT_EDIT_LANGUAGE.template);
  const pageMT = useRouteMatch(LINKS.PROJECT_LANGUAGES_MT.template);

  const isInIndex = pageLanguages?.isExact || pageEditLanguage?.isExact;

  const project = useProject();

  return (
    <BaseProjectView
      maxWidth={700}
      windowTitle={t('languages_title')}
      title={t('languages_title')}
      navigation={[
        [
          t('languages_title'),
          LINKS.PROJECT_LANGUAGES.build({
            [PARAMS.PROJECT_ID]: project.id,
          }),
        ],
      ]}
    >
      <StyledTabWrapper>
        <StyledTabs
          value={isInIndex ? 'languages' : pageMT?.isExact ? 'mt' : undefined}
        >
          <Tab
            value="languages"
            component={Link}
            to={LINKS.PROJECT_LANGUAGES.build({
              [PARAMS.PROJECT_ID]: project.id,
            })}
            label={t('languages_menu_project_languages')}
            data-cy="languages-menu-project-languages"
          />
          <Tab
            id="machine-translation-tab"
            value="mt"
            component={Link}
            to={LINKS.PROJECT_LANGUAGES_MT.build({
              [PARAMS.PROJECT_ID]: project.id,
            })}
            label={t('languages_menu_machine_translation')}
            data-cy="languages-menu-machine-translation"
            sx={{ overflow: 'visible' }}
          />
        </StyledTabs>
      </StyledTabWrapper>

      {isInIndex ? (
        <ProjectLanguages />
      ) : pageMT?.isExact ? (
        <MachineTranslation />
      ) : null}

      {pageEditLanguage?.isExact && <LanguageEditDialog />}
      {
        <QuickStartHighlightInPortal
          elId="machine-translation-tab"
          itemKey="machine_translation_tab"
        />
      }
    </BaseProjectView>
  );
};
