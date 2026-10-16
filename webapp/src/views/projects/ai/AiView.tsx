import { Box, styled, Tab, Tabs } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { Link, Redirect } from 'react-router-dom';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';
import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { BaseProjectView } from '../BaseProjectView';
import { useAiViewItems } from './aiViewItems';

const StyledTabs = styled(Tabs)`
  margin-bottom: -1px;
`;

const StyledTabWrapper = styled(Box)`
  border-bottom: 1px solid ${({ theme }) => theme.palette.divider1};
`;

export const AiView = () => {
  const project = useProject();
  const { t } = useTranslate();

  const { ActiveComponent, items, value } = useAiViewItems();
  const { preferredOrganization } = usePreferredOrganization();
  const projectOrganizationLoaded =
    preferredOrganization?.id === project.organizationOwner?.id;

  if (!ActiveComponent && projectOrganizationLoaded) {
    return (
      <Redirect
        to={LINKS.PROJECT_CONTEXT_DATA.build({
          [PARAMS.PROJECT_ID]: project.id,
        })}
      />
    );
  }

  return (
    <BaseProjectView
      windowTitle={t('ai_view_title')}
      title={t('ai_view_title')}
      maxWidth="normal"
      navigation={[
        [
          t('ai_view_title'),
          LINKS.PROJECT_TRANSLATIONS.build({
            [PARAMS.PROJECT_ID]: project.id,
          }),
        ],
      ]}
    >
      <StyledTabWrapper>
        <StyledTabs value={value ?? false}>
          {items.map((item) => {
            if (!item.tab.condition) {
              return null;
            }

            return (
              <Tab
                key={item.value}
                value={item.value}
                component={Link}
                to={item.link.build({
                  [PARAMS.PROJECT_ID]: project.id,
                })}
                label={item.tab.label}
                data-cy={item.tab.dataCy}
              />
            );
          })}
        </StyledTabs>
      </StyledTabWrapper>
      {ActiveComponent && <ActiveComponent />}
    </BaseProjectView>
  );
};
