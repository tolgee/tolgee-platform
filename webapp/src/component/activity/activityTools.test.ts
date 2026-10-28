import { describe, expect, it } from 'vitest';
import { buildActivity } from 'tg.component/activity/activityTools';
import { ActivityModel } from 'tg.component/activity/types';

const editProject = (modifications: Record<string, unknown>): ActivityModel =>
  ({
    revisionId: 1,
    timestamp: 0,
    type: 'EDIT_PROJECT',
    modifiedEntities: {
      Project: [{ entityId: 1, entityClass: 'Project', modifications }],
    },
  } as unknown as ActivityModel);

const titleKey = (model: ActivityModel) =>
  buildActivity(model).translation?.()?.props.keyName;

describe('EDIT_PROJECT activity title', () => {
  it('marks a project opened to community translation', () => {
    expect(titleKey(editProject({ public: { old: false, new: true } }))).toBe(
      'community_activity_project_opened'
    );
  });

  it('marks a project closed to community translation', () => {
    expect(titleKey(editProject({ public: { old: true, new: false } }))).toBe(
      'community_activity_project_closed'
    );
  });

  it('keeps the plain title for other project edits', () => {
    expect(titleKey(editProject({ name: { old: 'a', new: 'b' } }))).toBe(
      'activity_edit_project'
    );
  });
});
