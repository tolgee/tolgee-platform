import { Box, Button, styled, Typography } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { useMemo } from 'react';
import { RocketIcon } from 'tg.component/CustomIcons';
import { LINKS } from 'tg.constants/links';
import {
  useGlobalActions,
  useGlobalContext,
} from 'tg.globalContext/GlobalContext';
import { BottomLinks } from './BottomLinks';
import { QuickStartItem } from './QuickStartItem';
import { ItemType } from './types';

const items: ItemType[] = [
  {
    step: 'new_project',
    name: <T keyName="guide_new_project" />,
    actions: () => [
      { label: <T keyName="guide_new_project_demo" /> },
      {
        link: LINKS.PROJECTS.build(),
        label: <T keyName="guide_new_project_create" />,
        highlightItems: ['add_project'],
      },
    ],
  },
  {
    step: 'languages',
    name: <T keyName="guide_languages" />,
    needsProject: true,
    actions: () => [
      {
        label: <T keyName="guide_languages_set_up" />,
        highlightItems: [
          'menu_languages',
          'add_language',
          'machine_translation',
          'automatic_translation',
        ],
      },
    ],
  },
  {
    step: 'members',
    name: <T keyName="guide_members" />,
    needsProject: true,
    actions: () => [
      {
        label: <T keyName="guide_members_invite" />,
        highlightItems: ['menu_members', 'invitations', 'members'],
      },
    ],
  },
  {
    step: 'keys',
    name: <T keyName="guide_keys" />,
    needsProject: true,
    actions: () => [
      {
        label: <T keyName="guide_keys_add" />,
        highlightItems: ['menu_translations', 'add_key'],
      },
      {
        label: <T keyName="guide_keys_import" />,
        highlightItems: ['menu_import', 'pick_import_file'],
      },
    ],
  },
  {
    step: 'use',
    name: <T keyName="guide_use" />,
    needsProject: true,
    actions: () => [
      {
        label: <T keyName="guide_use_integrate" />,
        highlightItems: ['menu_integrate', 'integrate_form'],
      },
      {
        label: <T keyName="guide_use_export" />,
        highlightItems: ['menu_export', 'export_form'],
      },
    ],
  },
];

const StyledContainer = styled(Box)`
  display: grid;
  gap: 8px;
  grid-template-rows: auto 1fr auto;
  height: 100%;
`;

const StyledContent = styled(Box)`
  display: grid;
  gap: 8px;
  align-self: start;
`;

const StyledHeader = styled(Box)`
  display: flex;
  background: ${({ theme }) => theme.palette.emphasis[100]};
  border-radius: 0px 0px 16px 16px;
  font-size: 23px;
  font-weight: 400;
  padding: 13px 23px;
  align-items: center;
  gap: 12px;
`;

export const QuickStartGuide = () => {
  const { t } = useTranslate();
  const projectId = useGlobalContext((c) => c.quickStartGuide.lastProjectId);
  const completed = useGlobalContext((c) => c.quickStartGuide.completed);
  const { quickStartDismiss } = useGlobalActions();
  const allCompleted = useMemo(
    () => items.every((i) => completed.includes(i.step)),
    [completed, items]
  );

  return (
    <StyledContainer>
      <StyledHeader>
        <RocketIcon fontSize="small" />
        <T keyName="guide_title" />
      </StyledHeader>
      <StyledContent>
        {items.map((item, i) => (
          <QuickStartItem
            key={i}
            index={i + 1}
            item={item}
            projectId={projectId}
            done={completed.includes(item.step)}
          />
        ))}
        {allCompleted && (
          <Box sx={{ display: 'grid', justifyItems: 'center', gap: 2, pt: 1 }}>
            <Typography>{t('guide_finish_text')}</Typography>
            <Button
              color="primary"
              variant="contained"
              onClick={() => quickStartDismiss()}
            >
              {t('guide_finish_button')}
            </Button>
          </Box>
        )}
      </StyledContent>
      <BottomLinks allCompleted={allCompleted} />
    </StyledContainer>
  );
};
