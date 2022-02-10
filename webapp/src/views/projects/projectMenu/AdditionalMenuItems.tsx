import { Box, useTheme } from '@material-ui/core';
import List from '@material-ui/core/List';
import { SideMenuItem } from './SideMenuItem';
import { Feedback } from '@material-ui/icons';
import FeedbackFarm from '@feedbackfarm/react';
import { useTranslate } from '@tolgee/react';
import { useUser } from 'tg.hooks/useUser';

export function AdditionalMenuItems(props: { text: string }) {
  const t = useTranslate();
  const user = useUser();
  const theme = useTheme();

  return (
    <Box
      display="flex"
      flexGrow={1}
      flexDirection="column"
      justifyContent="flex-end"
      style={{ color: '#a9a9a9' }}
    >
      <List>
        <FeedbackFarm
          projectId="ya5byjbjr8W481d4W2Fs"
          identifier={user?.username}
          colors={{
            bug: {
              text: theme.palette.primary.contrastText,
              background: theme.palette.primary.main,
            },
            feature: {
              text: theme.palette.primary.contrastText,
              background: theme.palette.primary.main,
            },
            other: {
              text: theme.palette.primary.contrastText,
              background: theme.palette.primary.main,
            },
            send: {
              text: theme.palette.primary.contrastText,
              background: theme.palette.primary.main,
            },
          }}
          strings={{
            askTitle: t('feedback_form.ask_title'),
            askSubtitle: t('feedback_form.ask_subtitle'),
            conclusionTitle: t('feedback_form.conclusion_title'),
            conclusionSubtitle: t('feedback_form.conclusion_subtitle'),
            defaultError: t('feedback_form.error'),
            anotherFeedbackButton: t('feedback_form.another_feedback'),
            userIdentificationInputPlaceholder: t(
              'feedback_form.default_id_placeholder'
            ),
            sendButton: t('feedback_form.send'),
            textareaPlaceholders: {
              BUG: t('feedback_form.textarea.bug'),
              FEATURE: t('feedback_form.feature'),
              OTHER: t('feedback_form.other'),
              DEFAULT: t('feedback_form.default'),
            },
            feedbackTypes: {
              bug: t('feedback_form.types.bug'),
              feature: t('feedback_form.types.feature'),
              other: t('feedback_form.types.other'),
            },
          }}
        >
          <SideMenuItem
            listItemIconProps={{ style: { color: '#a9a9a9' } }}
            icon={<Feedback />}
            text={props.text}
          />
        </FeedbackFarm>
      </List>
    </Box>
  );
}
