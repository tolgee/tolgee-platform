import { useLocation } from 'react-router-dom';
import { queryDecode } from './useUrlSearchState';

export const useUrlSearch = () => {
  const search = useLocation().search;
  return queryDecode(search);
};

export const useUrlSearchArray = () => {
  const search = useLocation().search;
  return queryDecode(search, true) as Record<string, string[]>;
};
