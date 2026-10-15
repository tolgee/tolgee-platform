import { getRunPreviewState } from 'tg.ee.module/aiReview/utils/runPreview';

const counts = (willReview: number) => ({
  willReview,
  dryRunOnly: 0,
  alreadyHasResult: 0,
  humanReviewed: 0,
  noRule: 0,
  never: 0,
  openQaIssues: 0,
});

describe('getRunPreviewState', () => {
  it('switches counts with re-review', () => {
    const preview = {
      total: 10,
      withoutRereview: counts(2),
      withRereview: counts(5),
    };
    const without = getRunPreviewState(preview, false);
    expect(without.willReview).toBe(2);
    expect(without.skipped).toBe(8);
    expect(without.rereviewAdds).toBe(3);
    expect(getRunPreviewState(preview, true).willReview).toBe(5);
  });

  it('nothing to review until re-review adds some', () => {
    const preview = {
      total: 4,
      withoutRereview: counts(0),
      withRereview: counts(4),
    };
    expect(getRunPreviewState(preview, false).nothingToReview).toBe(true);
    expect(getRunPreviewState(preview, true).nothingToReview).toBe(false);
  });

  it('warns about large runs only', () => {
    const large = getRunPreviewState(
      {
        total: 6000,
        withoutRereview: counts(5000),
        withRereview: counts(5000),
      },
      false
    );
    expect(large.isLarge).toBe(true);

    const small = getRunPreviewState(
      {
        total: 20,
        withoutRereview: counts(4999),
        withRereview: counts(4999),
      },
      false
    );
    expect(small.isLarge).toBe(false);
  });
});
