import { isValidHttpUrl } from 'tg.fixtures/isValidUrl';
import { components } from 'tg.service/apiSchema.generated';

export type InContextValue = Pick<
  components['schemas']['CommunityPageRequest'],
  'communityInContext' | 'communityAppUrl'
>;

export const isInContextValueValid = ({
  communityInContext,
  communityAppUrl,
}: InContextValue) => !communityInContext || isAppUrlValid(communityAppUrl);

export const inContextValueToSave = <T extends InContextValue>(value: T): T => {
  const appUrl = isAppUrlValid(value.communityAppUrl)
    ? value.communityAppUrl?.trim()
    : '';
  return { ...value, communityAppUrl: appUrl && withProtocol(appUrl) };
};

// Must accept no more than the server's rule in CommunityPageRequest (backend/data), or saves fail with 400.
const isAppUrlValid = (appUrl?: string | null) => {
  const trimmed = appUrl?.trim();
  if (!trimmed) {
    return true;
  }
  const url = withProtocol(trimmed);
  return (
    /^https?:\/\/\S+$/.test(url) &&
    url.length <= 255 &&
    isValidHttpUrl(url) &&
    !new URL(url).username
  );
};

const withProtocol = (url: string) =>
  /^[a-z][a-z\d+.-]*:\/\//i.test(url) ? url : `https://${url}`;
