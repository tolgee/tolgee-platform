import { describe, expect, it } from 'vitest';
import { yearsSince } from 'tg.views/projects/community/contributors/useContributorsFilters';

const now = new Date(2026, 10, 19);

describe('yearsSince', () => {
  it('lists every year from the start to now, newest first', () => {
    expect(yearsSince(new Date(2019, 5, 1), now)).toEqual([
      2026, 2025, 2024, 2023, 2022, 2021, 2020, 2019,
    ]);
  });

  it('lists only the current year for a start this year or later', () => {
    expect(yearsSince(new Date(2026, 0, 1), now)).toEqual([2026]);
    expect(yearsSince(new Date(2027, 0, 1), now)).toEqual([2026]);
  });
});
