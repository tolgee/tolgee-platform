import { useHistory, useLocation } from 'react-router';

function queryEncode(data: Record<string, any>) {
  const queries: string[] = [];
  Object.entries(data).forEach(([key, value]) => {
    if (value !== undefined) {
      queries.push(`${encodeURIComponent(key)}=${encodeURIComponent(value)}`);
    }
  });
  return queries.join('&');
}

function queryDecode(url: string) {
  const query = (url || '').substr(1);
  const result = {};
  if (query) {
    query.split('&').forEach(function (part) {
      const item = part.split('=');
      result[decodeURIComponent(item[0])] = decodeURIComponent(item[1]);
    });
  }
  return result;
}

export const useQueryState = (
  key: string,
  defaultVal?: string,
  history = false
): [string, (value: string) => void] => {
  const location = useLocation();
  const value = queryDecode(location.search)[key];
  const { replace, push } = useHistory();

  const getNewSearch = (value: any) => {
    const data = queryDecode(location.search);
    const newValue = value === defaultVal ? undefined : value;
    let newSearch = queryEncode({
      ...data,
      [key]: newValue,
    });
    if (newSearch) {
      newSearch = '?' + newSearch;
    }
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

  return [value === undefined ? defaultVal : value, setState];
};
