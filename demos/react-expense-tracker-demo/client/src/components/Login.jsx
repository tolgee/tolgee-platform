import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import axios from 'axios';
import { useTranslate } from '@tolgee/react';
import { LangSelector } from './LangSelector';

function Login() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();
  const { t } = useTranslate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const response = await axios.post('http://localhost:5000/api/login', { email, password });
      localStorage.setItem('token', response.data.token);
      localStorage.setItem('userId', response.data.userId);
      navigate('/dashboard');
    } catch (error) {
      if (error.response.data.error === "Invalid email or password") {
        setError(t('invalid_email_or_password', 'Invalid email or password'));
      }
      else {
        setError(error.response.data.error || 'An error occurred during login');
      }
    }
  };

  return (
    <>
      <LangSelector />
      <div className="flex items-center justify-center bg-gray-100">
        <div className="px-8 py-6 mt-4 text-left bg-white shadow-lg">
          <h3 className="text-2xl font-bold text-center">{t('login_to_your_account', 'Login to your account')}</h3>
          <form onSubmit={handleSubmit}>
            <div className="mt-4">
              <div>
                <label className="block" htmlFor="email">{t('email', 'Email')}</label>
                <input type="email" placeholder={t('email', 'Email')}
                  className="w-full px-4 py-2 mt-2 border rounded-md focus:outline-none focus:ring-1 focus:ring-blue-600"
                  value={email} onChange={(e) => setEmail(e.target.value)} required />
              </div>
              <div className="mt-4">
                <label className="block">{t('password', 'Password')}</label>
                <input type="password" placeholder={t('password', 'Password')}
                  className="w-full px-4 py-2 mt-2 border rounded-md focus:outline-none focus:ring-1 focus:ring-blue-600"
                  value={password} onChange={(e) => setPassword(e.target.value)} required />
              </div>
              <div className="flex items-baseline justify-between">
                <button className="px-6 py-2 mt-4 text-white bg-blue-600 rounded-lg hover:bg-blue-900">{t('login', 'Login')}</button>
                <Link to="/register" className="text-sm text-blue-600 hover:underline">{t('register', 'Register')}</Link>
              </div>
            </div>
          </form>
          {error && <p className="text-red-500 text-sm mt-2">{t('error', error)}</p>}
        </div>
      </div>
    </>
  );
}

export default Login;