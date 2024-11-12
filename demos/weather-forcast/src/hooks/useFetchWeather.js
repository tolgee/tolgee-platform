import { useQuery } from '@tanstack/react-query';
import { fetchWeatherByCity, fetchWeatherByCoords } from '../services/api';

export function useFetchWeather(geoData, searchQuery) {
  const { data, error, isLoading } = useQuery({
    queryKey: ['weather', searchQuery || geoData],
    queryFn: () =>
      searchQuery
        ? fetchWeatherByCity(searchQuery)
        : fetchWeatherByCoords(geoData),
    enabled: (!!geoData?.latitude && !!geoData?.longitude) || !!searchQuery,
    staleTime: 60 * 60 * 1000,
    cacheTime: 60 * 60 * 1000,
  });

  return { data, error, isLoading };
}
