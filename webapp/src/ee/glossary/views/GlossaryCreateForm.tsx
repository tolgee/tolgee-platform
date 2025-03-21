import { VFC } from 'react';
import { TextField } from 'tg.component/common/form/fields/TextField';
import Box from '@mui/material/Box';
import { useTranslate } from '@tolgee/react';
import { BaseLanguageSelect } from 'tg.views/projects/project/components/BaseLanguageSelect';
import { ProjectSelect } from 'tg.ee.module/glossary/components/ProjectSelect';

type Props = {
  disabled?: boolean;
};

export const GlossaryCreateForm: VFC<Props> = ({ disabled }) => {
  const { t } = useTranslate();

  return (
    <Box display="grid">
      <TextField
        name="name"
        label={t('create_glossary_field_name')}
        placeholder={t('glossary_default_name')}
        data-cy="create-glossary-field-name"
        disabled={disabled}
      />
      <BaseLanguageSelect
        name="baseLanguageCode"
        label={t('create_glossary_field_base_language')}
        valueKey="tag"
        minHeight
        languages={[
          // TODO: load list of all languages in use by organization from backend
          {
            tag: 'en',
            name: 'English',
            originalName: 'English',
            flagEmoji: '🇬🇧',
          },
          {
            tag: 'es',
            name: 'Spanish',
            originalName: 'Español',
            flagEmoji: '🇪🇸',
          },
          {
            tag: 'de',
            name: 'German',
            originalName: 'Deutsch',
            flagEmoji: '🇩🇪',
          },
          {
            tag: 'fr',
            name: 'French',
            originalName: 'Français',
            flagEmoji: '🇫🇷',
          },
          {
            tag: 'it',
            name: 'Italian',
            originalName: 'Italiano',
            flagEmoji: '🇮🇹',
          },
          {
            tag: 'ja',
            name: 'Japanese',
            originalName: '日本語',
            flagEmoji: '🇯🇵',
          },
          {
            tag: 'zh',
            name: 'Chinese',
            originalName: '中文',
            flagEmoji: '🇨🇳',
          },
          {
            tag: 'ru',
            name: 'Russian',
            originalName: 'Русский',
            flagEmoji: '🇷🇺',
          },
        ]}
      />
      <ProjectSelect
        name="assignedProjects"
        label={t('create_glossary_field_project')}
        key="id"
        available={[
          // TODO: load list of all organization projects available to user
          {
            id: 1,
            name: 'Test Project',
            slug: 'test-project',
            icuPlaceholders: true,
            avatar: undefined,
            baseLanguage: undefined,
          },
          {
            id: 2,
            name: 'Internationalization Project',
            slug: 'i18n-project',
            icuPlaceholders: false,
            avatar: undefined,
            baseLanguage: {
              id: 1,
              base: true,
              tag: 'en',
              name: 'English',
              originalName: 'English',
              flagEmoji: '🇬🇧',
            },
          },
          {
            id: 3,
            name: 'E-commerce App',
            slug: 'ecommerce-app',
            icuPlaceholders: true,
            avatar: undefined,
            baseLanguage: {
              id: 2,
              base: true,
              tag: 'de',
              name: 'German',
              originalName: 'Deutsch',
              flagEmoji: '🇩🇪',
            },
          },
          {
            id: 4,
            name: 'Social Media Platform',
            slug: 'social-media',
            icuPlaceholders: false,
            avatar: undefined,
            baseLanguage: {
              id: 3,
              base: true,
              tag: 'es',
              name: 'Spanish',
              originalName: 'Español',
              flagEmoji: '🇪🇸',
            },
          },
          {
            id: 5,
            name: 'Educational Portal',
            slug: 'edu-portal',
            icuPlaceholders: true,
            avatar: undefined,
            baseLanguage: {
              id: 4,
              base: true,
              tag: 'fr',
              name: 'French',
              originalName: 'Français',
              flagEmoji: '🇫🇷',
            },
          },
        ]}
      />
    </Box>
  );
};
