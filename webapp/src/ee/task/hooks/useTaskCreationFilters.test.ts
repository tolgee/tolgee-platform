import { describe, expect, it } from 'vitest';

import { taskScopeFiltersQuery } from './useTaskCreationFilters';

describe('taskScopeFiltersQuery', () => {
  it('maps each status onto its own query param', () => {
    expect(
      taskScopeFiltersQuery(
        { filterTaskStatus: 'NOT_IN_OPEN_TASK' },
        'TRANSLATE'
      )
    ).toMatchObject({
      filterNotInOpenTask: true,
      filterNeverInTask: false,
      filterHasBeenInTask: false,
    });

    expect(
      taskScopeFiltersQuery({ filterTaskStatus: 'NEVER_IN_TASK' }, 'TRANSLATE')
    ).toMatchObject({
      filterNeverInTask: true,
      filterNotInOpenTask: false,
      filterHasBeenInTask: false,
    });

    expect(
      taskScopeFiltersQuery(
        { filterTaskStatus: 'HAS_BEEN_IN_TASK' },
        'TRANSLATE'
      )
    ).toMatchObject({
      filterHasBeenInTask: true,
      filterNotInOpenTask: false,
      filterNeverInTask: false,
    });
  });

  it('sends no status when none is selected', () => {
    expect(taskScopeFiltersQuery({}, 'TRANSLATE')).toMatchObject({
      filterNeverInTask: false,
      filterHasBeenInTask: false,
      filterNotInOpenTask: false,
    });
  });

  it('always scopes to the type being created', () => {
    expect(taskScopeFiltersQuery({}, 'REVIEW').filterTaskType).toEqual([
      'REVIEW',
    ]);
    expect(
      taskScopeFiltersQuery({ filterTaskType: ['TRANSLATE'] }, 'REVIEW')
        .filterTaskType
    ).toEqual(['TRANSLATE', 'REVIEW']);
    expect(
      taskScopeFiltersQuery({ filterTaskType: ['REVIEW'] }, 'REVIEW')
        .filterTaskType
    ).toEqual(['REVIEW']);
  });
});
