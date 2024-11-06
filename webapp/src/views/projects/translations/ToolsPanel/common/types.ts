import { components } from 'tg.service/apiSchema.generated';
import { DeletableKeyWithTranslationsModelType } from '../../context/types';
import { LanguageModel } from 'tg.component/PermissionsSettings/types';
import type { useProjectPermissions } from 'tg.hooks/useProjectPermissions';

export type ProjectModel = components['schemas']['ProjectModel'];
export type TranslationViewModel =
  components['schemas']['TranslationViewModel'];

export type PanelContentData = {
  project: ProjectModel;
  keyData: DeletableKeyWithTranslationsModelType;
  language: LanguageModel;
  baseLanguage: LanguageModel;
  activeVariant: string | undefined;
  editEnabled: boolean;
  projectPermissions: ReturnType<typeof useProjectPermissions>;
};

export type PanelContentProps = PanelContentData & {
  setItemsCount: (value: number | undefined) => void;
  setValue: (value: string) => void;
};

export type PanelConfig = {
  id: string;
  icon: React.ReactNode;
  name: React.ReactNode;
  component: React.FC<PanelContentProps>;
  itemsCountFunction?: (props: PanelContentData) => number | React.ReactNode;
  displayPanel?: (value: PanelContentData) => boolean;
  hideWhenCountZero?: boolean;
  hideCount?: boolean;
};
