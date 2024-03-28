// get CI_RELEASE env variable
const CI_RELEASE = process.env.CI_RELEASE === 'true';
export const GLOBAL_RETRIES = CI_RELEASE ? 10 : 0;
