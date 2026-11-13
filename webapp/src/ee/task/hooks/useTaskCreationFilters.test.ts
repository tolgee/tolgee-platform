import { describe, expect, it } from 'vitest';

import { taskScopeFiltersQuery } from './useTaskCreationFilters';

describe('taskScopeFiltersQuery', () => {
  it('maps each task type onto its own condition', () => {
    expect(
      taskScopeFiltersQuery(
        {
          filterTaskStatus: {
            TRANSLATE: 'HAS_BEEN_IN_TASK',
            REVIEW: 'NEVER_IN_TASK',
          },
        },
        'REVIEW'
      ).filterTaskInStatus
    ).toEqual(['TRANSLATE,HAS_BEEN_IN_TASK', 'REVIEW,NEVER_IN_TASK']);
  });

  it('keeps the two types independent', () => {
    expect(
      taskScopeFiltersQuery(
        { filterTaskStatus: { REVIEW: 'IN_OPEN_TASK' } },
        'TRANSLATE',
        false
      ).filterTaskInStatus
    ).toEqual(['REVIEW,IN_OPEN_TASK']);
  });

  it('always constrains the type being created', () => {
    // nothing chosen for TRANSLATE, but creation drops its open-task conflicts regardless
    expect(
      taskScopeFiltersQuery(
        { filterTaskStatus: { REVIEW: 'HAS_BEEN_IN_TASK' } },
        'TRANSLATE'
      ).filterTaskInStatus
    ).toEqual(['REVIEW,HAS_BEEN_IN_TASK', 'TRANSLATE,NOT_IN_OPEN_TASK']);

    expect(taskScopeFiltersQuery({}, 'REVIEW').filterTaskInStatus).toEqual([
      'REVIEW,NOT_IN_OPEN_TASK',
    ]);
  });

  it('keeps a stricter explicit condition on the created type', () => {
    expect(
      taskScopeFiltersQuery(
        { filterTaskStatus: { TRANSLATE: 'NEVER_IN_TASK' } },
        'TRANSLATE'
      ).filterTaskInStatus
    ).toEqual(['TRANSLATE,NEVER_IN_TASK']);
  });

  it('drops a condition the created type cannot carry', () => {
    // picked while REVIEW was not being created, then the form switched onto it: the submenu
    // offers no matching option, and sending it would preview zero keys for every language
    expect(
      taskScopeFiltersQuery(
        { filterTaskStatus: { REVIEW: 'IN_OPEN_TASK' } },
        'REVIEW'
      ).filterTaskInStatus
    ).toEqual(['REVIEW,NOT_IN_OPEN_TASK']);

    expect(
      taskScopeFiltersQuery(
        { filterTaskStatus: { TRANSLATE: 'HAS_BEEN_IN_TASK' } },
        'TRANSLATE'
      ).filterTaskInStatus
    ).toEqual(['TRANSLATE,NOT_IN_OPEN_TASK']);
  });

  it('leaves the created type alone when creation applies no condition', () => {
    expect(
      taskScopeFiltersQuery(
        { filterTaskStatus: { REVIEW: 'IN_OPEN_TASK' } },
        'REVIEW',
        false
      ).filterTaskInStatus
    ).toEqual(['REVIEW,IN_OPEN_TASK']);
  });
});
