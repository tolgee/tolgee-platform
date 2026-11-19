import { afterEach, describe, expect, it } from 'vitest';
import { yearsSince } from 'tg.component/community/yearsSince';

const now = new Date(2026, 10, 19);

describe('yearsSince', () => {
  it('lists every year from the earliest start to now, newest first', () => {
    expect(
      yearsSince([Date.UTC(2023, 2, 1), Date.UTC(2019, 5, 1), undefined], now)
    ).toEqual([2026, 2025, 2024, 2023, 2022, 2021, 2020, 2019]);
  });

  describe('takes the year of the start in UTC, as the server files days', () => {
    const originalTz = process.env.TZ;
    afterEach(() => {
      process.env.TZ = originalTz;
    });

    it('east of UTC', () => {
      process.env.TZ = 'Asia/Tokyo';
      expect(yearsSince([Date.UTC(2024, 11, 31, 23, 30)], now)).toEqual([
        2026, 2025, 2024,
      ]);
    });

    it('west of UTC', () => {
      process.env.TZ = 'America/New_York';
      expect(yearsSince([Date.UTC(2025, 0, 1, 0, 30)], now)).toEqual([
        2026, 2025,
      ]);
    });
  });

  it('lists only the current year without a past start', () => {
    expect(yearsSince([], now)).toEqual([2026]);
    expect(yearsSince([undefined], now)).toEqual([2026]);
    expect(yearsSince([Date.UTC(2027, 0, 1)], now)).toEqual([2026]);
  });
});
