import { useState, useEffect } from "react";
import { T, useTranslate } from "@tolgee/react";

import "./App.css";

import { FaMapMarkerAlt } from "react-icons/fa";
import { FaDroplet, FaWind } from "react-icons/fa6";
import { BiSearch } from "react-icons/bi";

import LanguageSelect from "./components/LanguageSelect";
import RippleLoader from "./components/Loader";

interface WeatherData {
  name: string;
  main: {
    temp: number;
    humidity: number;
  };
  weather: {
    description: string;
    icon: string;
  }[];
  wind: {
    speed: number;
  };
}

interface ForecastData {
  list: {
    dt: number;
    main: {
      temp: number;
    };
    weather: {
      icon: string;
    }[];
  }[];
}

function App() {
  const [city, setCity] = useState<string>("");
  const [weatherData, setWeatherData] = useState<WeatherData | null>(null);
  const [forecastData, setForecastData] = useState<ForecastData | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const { t } = useTranslate();

  const fetchWeatherData = async (cityName: string) => {
    const apiKey = process.env.WEATHER_API_KEY;
    const currentWeatherUrl = `https://api.openweathermap.org/data/2.5/weather?q=${cityName}&appid=${apiKey}&units=metric`;
    const forecastUrl = `https://api.openweathermap.org/data/2.5/forecast?q=${cityName}&appid=${apiKey}&units=metric`; // Forecast URL

    try {
      setLoading(true);
      setError(null);

      // Fetch current weather
      const weatherResponse = await fetch(currentWeatherUrl);
      if (!weatherResponse.ok) {
        throw new Error("City not found! Try another one");
      }
      const weatherData: WeatherData = await weatherResponse.json();
      setWeatherData(weatherData);

      // Fetch forecast data
      const forecastResponse = await fetch(forecastUrl);
      if (!forecastResponse.ok) {
        throw new Error("Unable to fetch forecast data");
      }
      const forecastData: ForecastData = await forecastResponse.json();
      setForecastData(forecastData);

    } catch (error) {
      if (error instanceof Error) {
        setError(error.message);
      }
      setWeatherData(null);
      setForecastData(null);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    fetchWeatherData(city);
    setCity("");
  };

  useEffect(() => {
    const getUserLocation = async () => {
      if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
          async (position) => {
            const { latitude, longitude } = position.coords;
            const apiKey = process.env.WEATHER_API_KEY;
            const reverseGeocodeUrl = `https://api.openweathermap.org/data/2.5/weather?lat=${latitude}&lon=${longitude}&appid=${apiKey}&units=metric`;

            try {
              setLoading(true);
              const response = await fetch(reverseGeocodeUrl);
              if (!response.ok) {
                throw new Error("Unable to fetch location data");
              }
              const data: WeatherData = await response.json();
              setWeatherData(data);
              setCity(data.name);
              await fetchWeatherData(data.name);
            } catch (error) {
              if (error instanceof Error) {
                setError(error.message);
              }
              setWeatherData(null);
              setForecastData(null);
            } finally {
              setLoading(false);
            }
          },
          () => {
            setError("Unable to retrieve your location");
            setWeatherData(null);
            setForecastData(null);
          }
        );
      } else {
        setError("Geolocation is not supported by this browser.");
        setWeatherData(null);
        setForecastData(null);
      }
    };

    getUserLocation();
  }, []);

  return (
    <>
      <section className="flex flex-col items-center justify-center min-h-screen px-3">
        <LanguageSelect />
        <div className="mt-3 bg-slate-400/50 rounded shadow-lg border border-white/30 p-5 w-full md:w-[350px]">
          <div>
            <form className="relative" onSubmit={handleSubmit}>
              <input
                type="text"
                name="search"
                placeholder={t("search_city", "Search City")}
                className="w-full px-4 py-2 border rounded-full text-white/70 bg-[#668ba0] focus:outline-none border-transparent focus:border-[#668ba0]"
                value={city}
                onChange={(e) => setCity(e.target.value)}
              />
              <button type="submit" className="absolute top-3 right-3">
                <BiSearch className="text-white/70" size={20} />
              </button>
            </form>
          </div>

          {loading && (
            <div className="flex justify-center items-center">
              <RippleLoader />
            </div>
          )}
          {error && (
            <p className="text-red-600 font-bold flex justify-center items-center">
              {error}
            </p>
          )}

          {!error && weatherData && (
            <div>
              <div className="flex justify-between items-center text-white font-bold">
                <span className="flex items-center gap-x-2">
                  <FaMapMarkerAlt size={20} />
                  <p className="text-xl font-serif">{weatherData.name}</p>
                </span>

                <div className="flex flex-col items-center">
                <img
                  src={`https://openweathermap.org/img/wn/${weatherData.weather[0].icon}@2x.png`}
                  alt="weather icon"
                />
                <span>
                  <p className="text-6xl font-bold text-white">
                    {Math.round(weatherData.main.temp)} °C
                  </p>
                </span>
              </div>
              </div>

              <div className="my-5 flex justify-between items-center">
                <div className="flex items-center gap-x-3">
                  <FaDroplet size={30} className="text-white/90" />
                  <span>
                    <p className="text-lg font-serif text-white font-bold">
                      <T keyName="humidity">Humidity</T>
                    </p>
                    <p className="text-lg font-medium text-white/90">
                      {weatherData.main.humidity}%
                    </p>
                  </span>
                </div>
                <div className="flex w-1/2 items-center gap-x-3">
                  <FaWind size={30} className="text-white/90" />
                  <span className="">
                    <p className="text-lg font-serif text-white font-bold">
                      <T keyName="wind_speed">Wind Speed</T>
                    </p>
                    <p className="text-lg font-medium text-white/90">
                      {weatherData.wind.speed} m/s
                    </p>
                  </span>
                </div>
              </div>

              {/* Forecast for the next 3 days */}
              {forecastData && (
                <div className="mt-3">
                  <h2 className="text-lg font-bold text-white text-center">
                    <T keyName="forecast">Forecast for the next 3 days</T>
                  </h2>
                  <div className="grid grid-cols-3 ">
                    {forecastData.list.slice(0, 3).map((forecast) => (
                      <div key={forecast.dt} className="text-center text-white">
                        <img
                          src={`https://openweathermap.org/img/wn/${forecast.weather[0].icon}@2x.png`}
                          alt="forecast icon"
                        />
                        <p className="text-lg font-bold">
                          {Math.round(forecast.main.temp)} °C
                        </p>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      </section>
    </>
  );
}

export default App;
