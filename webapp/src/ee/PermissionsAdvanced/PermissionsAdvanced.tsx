import {
  HierarchyItem,
  LanguageModel,
  PermissionAdvancedState,
} from 'tg.component/PermissionsSettings/types';
import { Hierarchy } from './Hierarchy';
import { useTranslate } from '@tolgee/react';
import { useEnabledFeatures } from 'tg.globalContext/helpers';
import { PaidFeatureBanner } from '../common/PaidFeatureBanner';

type Props = {
  dependencies: HierarchyItem;
  state: PermissionAdvancedState;
  onChange: (value: PermissionAdvancedState) => void;
  allLangs?: LanguageModel[];
};

export const PermissionsAdvanced: React.FC<Props> = ({
  dependencies,
  state,
  onChange,
  allLangs,
}) => {
  const { t } = useTranslate();
  const { isEnabled } = useEnabledFeatures();

  if (!isEnabled('GRANULAR_PERMISSIONS')) {
    return <PaidFeatureBanner />;
  }

  return (
    <Hierarchy
      dependencies={dependencies}
      state={state}
      onChange={onChange}
      allLangs={allLangs}
      structure={{
        value: 'admin',
        children: [
          {
            label: t('permissions_item_keys'),
            children: [
              {
                value: 'keys.view',
              },
              {
                value: 'keys.create',
              },
              {
                value: 'keys.edit',
              },
              {
                value: 'keys.delete',
              },
            ],
          },
          {
            label: t('permissions_item_translations'),
            children: [
              {
                value: 'translations.view',
              },
              {
                value: 'translations.edit',
              },
              {
                value: 'translations.state-edit',
              },
              {
                label: t('permissions_item_translations_comments'),
                children: [
                  {
                    value: 'translation-comments.add',
                  },
                  {
                    value: 'translation-comments.edit',
                  },
                  {
                    value: 'translation-comments.set-state',
                  },
                ],
              },
            ],
          },
          {
            label: t('permissions_item_screenshots'),
            children: [
              {
                value: 'screenshots.view',
              },
              {
                value: 'screenshots.upload',
              },
              {
                value: 'screenshots.delete',
              },
            ],
          },
          {
            label: t('permissions_item_batch_operations'),
            children: [
              { value: 'batch-jobs.view' },
              { value: 'batch-jobs.cancel' },
              { value: 'translations.batch-by-tm' },
              { value: 'translations.batch-machine' },
            ],
          },
          {
            label: t('permissions_item_members'),
            children: [
              {
                value: 'members.view',
              },
              {
                value: 'members.edit',
              },
            ],
          },
          {
            value: 'project.edit',
          },
          {
            value: 'activity.view',
          },
          {
            value: 'languages.edit',
          },
        ],
      }}
    />
  );
};
