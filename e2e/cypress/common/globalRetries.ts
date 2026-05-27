// Retries are enabled on release runs, and on flaky-detection runs
// (the reportIntermittentTests workflow) so that a test which fails then
// passes can be identified as flaky and reported.
const CI_RELEASE = process.env.CI_RELEASE === 'true';
const DETECT_FLAKY_TESTS = process.env.DETECT_FLAKY_TESTS === 'true';
export const GLOBAL_RETRIES = CI_RELEASE || DETECT_FLAKY_TESTS ? 10 : 0;
