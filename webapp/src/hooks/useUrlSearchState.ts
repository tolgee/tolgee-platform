import { useHistory, useLocation } from 'react-router';

export function queryEncode(data: Record<string, any>) {
  const queries: string[] = [];
  Object.entries(data).forEach(([key, value]) => {
    if (value !== undefined) {
      if (Array.isArray(value)) {
        value.forEach((val) => {
          queries.push(`${encodeURIComponent(key)}=${encodeURIComponent(val)}`);
        });
      } else {
        queries.push(`${encodeURIComponent(key)}=${encodeURIComponent(value)}`);
      }
    }
  });
  const result = queries.join('&');
  return result ? '?' + result : '';
}

export function queryDecode(url: string, forceArray = false) {
  const query = (url || '').substr(1);
  const result: Record<string, string | string[]> = {};
  if (query) {
    query.split('&').forEach(function (part) {
      const item = part.split('=');
      const key = decodeURIComponent(item[0]);
      const value = decodeURIComponent(item[1]);

      if (result[key]) {
        if (Array.isArray(result[key])) {
          (result[key] as string[]).push(value);
        } else {
          result[key] = [result[key] as string, value];
        }
      } else {
        if (forceArray) {
          result[key] = [value];
        } else {
          result[key] = value;
        }
      }
    });
  }
  return result;
}

type Options = {
  defaultVal?: string | string[];
  history?: boolean;
  array?: boolean;
};

export const useUrlSearchState = (
  key: string,
  options?: Options
): [
  string | string[] | undefined,
  (value: string | string[] | undefined) => void
] => {
  const location = useLocation();
  const value = queryDecode(location.search)[key];
  const { replace, push } = useHistory();

  const getNewSearch = (value: any) => {
    const data = queryDecode(location.search);
    const newValue = value === options?.defaultVal ? undefined : value;
    const newSearch = queryEncode({
      ...data,
      [key]: newValue,
    });

    return newSearch;
  };

  const setState = (value: any) => {
    const newSearch = getNewSearch(value);
    if (history) {
      push(location.pathname + newSearch);
    } else {
      replace(location.pathname + newSearch);
    }
  };

  if (!options?.array) {
    return [value === undefined ? options?.defaultVal : value, setState];
  } else {
    return [
      Array.isArray(value) ? value : value !== undefined ? [value] : [],
      setState,
    ];
  }
};
