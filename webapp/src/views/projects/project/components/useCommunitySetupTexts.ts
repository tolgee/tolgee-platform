import { useTranslate } from '@tolgee/react';

export type CommunitySetupStep =
  | 'description'
  | 'guidelines'
  | 'share'
  | 'inContext';

type StepTexts = {
  label: string;
  short: string;
  hint: string;
};

type TextFieldTexts = {
  fieldLabel: string;
  placeholder: string;
};

export const useCommunitySetupTexts = (): Record<
  CommunitySetupStep,
  StepTexts
> &
  Record<'descriptionField' | 'guidelinesField', TextFieldTexts> => {
  const { t } = useTranslate();

  return {
    descriptionField: {
      fieldLabel: t(
        'community_settings_description_label',
        'Description (markdown)'
      ),
      placeholder: t(
        'community_settings_description_placeholder',
        'Say what the project is and add a link to your website or repository.'
      ),
    },
    guidelinesField: {
      fieldLabel: t(
        'community_settings_guidelines_label',
        'Rules and tips (markdown)'
      ),
      placeholder: t(
        'community_guidelines_default',
        'Click on a language, sign in and start translating. Suggestions are used once the project team approves them. Thanks for helping!'
      ),
    },
    description: {
      label: t('community_settings_project_description', 'Project description'),
      short: t(
        'community_settings_project_description_purpose',
        'Main info about the project, important links'
      ),
      hint: t(
        'community_setup_hint_description',
        'Introduce the project and add links where people can learn more.'
      ),
    },
    guidelines: {
      label: t('community_settings_guidelines_short', 'How to contribute'),
      short: t(
        'community_settings_guidelines_purpose',
        'Rules and extra info for contributors'
      ),
      hint: t(
        'community_setup_hint_guidelines',
        'Tell contributors what to keep in mind, like tone, terms or rules.'
      ),
    },
    share: {
      label: t('community_settings_share', 'Share it'),
      short: t(
        'community_settings_share_purpose',
        'Link and badge for your README or website'
      ),
      hint: t(
        'community_setup_hint_share',
        'Add the link or badge to your README and website.'
      ),
    },
    inContext: {
      label: t('community_settings_in_context', 'In-context translation'),
      short: t(
        'community_settings_in_context_purpose',
        'For web apps: translate right in the app'
      ),
      hint: t(
        'community_setup_hint_in_context',
        'Let contributors translate right in your web app.'
      ),
    },
  };
};
