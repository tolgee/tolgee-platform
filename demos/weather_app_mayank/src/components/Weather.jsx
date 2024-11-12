/* eslint-disable no-unused-vars */
import { useState } from 'react';
import { useFetchWeather } from '../hooks/useFetchWeather';
import useGeolocation from '../hooks/useGeolocation';
import { WeatherCard } from './WeatherCard';
import { Forecast } from './Forecast';
import { useTranslate } from '@tolgee/react';

export default function Weather() {
  const { loading, error, data: geoData } = useGeolocation();
  const [city, setCity] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const {
    data,
    error: apiError,
    isLoading: apiLoading,
  } = useFetchWeather(geoData, searchQuery);
  const { t } = useTranslate();
  if (loading) {
    return <p className='text-blue-500 text-lg font-semibold'>{t('Loading')}  ...</p>;
  }

  const { currentWeather, forecast } = data || {};

  const handleSearch = (e) => {
    e.preventDefault();
    if (city.trim()) {
      console.log('city=', city);
      setSearchQuery(city.trim());
    }
  };


  return (
    <div>
      {error && <p>{t('not-allow')} </p>}
      {apiError && <p>{apiError.message}</p>}
      <div className='bg-black shadow-md p-2 rounded-lg mb-4 w-full  border-2 border-red-500'>
        <form onSubmit={handleSearch}>
          <input
            type='text'
            placeholder={t('enter-city')} 
            className='p-2 border bg-white text-black border-gray-300 rounded '
            value={city}
            onChange={(e) => setCity(e.target.value)}
          />
          <button
            type='submit'
            className='ml-2 p-2 bg-blue-500 text-white rounded'
          >
            {t('search-key', 'Search')}
          </button>
        </form>
      </div>

      {currentWeather && (
        <div className='bg-black text-white shadow-md p-6 rounded-lg mb-4 w-full border-2 border-red-500'>
          <WeatherCard data={currentWeather} />
        </div>
      )}

      {forecast && (
        <div className='bg-black text-white shadow-md p-6 rounded-lg mb-4 w-full border-2 border-red-500'>
          <Forecast forecast={forecast} />
        </div>
      )}
    </div>
  );
}
