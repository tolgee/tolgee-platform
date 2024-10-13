import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import axios from 'axios';
import { useTranslate } from '@tolgee/react';
import { LangSelector } from './LangSelector';

function Register() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const navigate = useNavigate();
  const { t } = useTranslate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      await axios.post('http://localhost:5000/api/register', { email, password });
      navigate('/login');
    } catch (error) {
      if (error.response.data.error === "User already exists") {
        setError(t('user_already_exists', 'User already exists'));
      } else {
        setError(error.response.data.error || 'An error occurred during registration');
      }
    }
  };

  return (
    <>
      <LangSelector />
      <div className="flex items-center justify-center bg-gray-100">
        <div className="px-8 py-6 mt-4 text-left bg-white shadow-lg">
          <h3 className="text-2xl font-bold text-center">{t('register_new_account', 'Register a new account')}</h3>
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
                <button className="px-6 py-2 mt-4 text-white bg-blue-600 rounded-lg hover:bg-blue-900">{t('register', 'Register')}</button>
                <Link to="/login" className="text-sm text-blue-600 hover:underline">{t('login', 'Login')}</Link>
              </div>
            </div>
          </form>
          {error && <p className="text-red-500 text-sm mt-2">{t('registration_error', error)}</p>}
        </div>
      </div>
    </>
  );
}

export default Register;