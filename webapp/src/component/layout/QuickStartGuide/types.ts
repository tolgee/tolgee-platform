export type Action = {
  link?: string;
  label: React.ReactNode;
  highlightItems?: HighlightItem[];
};

export type ItemStep = 'new_project' | 'languages' | 'members' | 'keys' | 'use';
export type ItemType = {
  step: ItemStep;
  name: React.ReactNode;
  actions?: (data: { projectId: number | undefined }) => Action[];
  needsProject?: boolean;
};

export type HighlightItem =
  | 'menu_languages'
  | 'menu_members'
  | 'menu_translations'
  | 'menu_settings'
  | 'menu_import'
  | 'menu_export'
  | 'menu_integrate'
  | 'add_project'
  | 'add_language'
  | 'machine_translation'
  | 'invitations'
  | 'members'
  | 'add_key'
  | 'pick_import_file'
  | 'add_project_submit'
  | 'export_form'
  | 'integrate_form'
  | 'demo_project';
