import { useEffect, useState } from 'react';

export default function useGeolocation() {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [data, setData] = useState({});

  useEffect(() => {
    const onSucces = (e) => {
      setLoading(false);
      setError(null);
      setData(e.coords);
    };

    const onError = (e) => {
      setError(e);
      setLoading(false);
    };
    navigator.geolocation.getCurrentPosition(onSucces, onError);
  }, []);

  return { loading, error, data };
}
