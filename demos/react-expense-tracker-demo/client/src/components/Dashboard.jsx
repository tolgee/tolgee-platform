import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';
import { LangSelector } from './LangSelector';
import { useTranslate } from '@tolgee/react';

function Dashboard() {
  const [expenses, setExpenses] = useState([]);
  const [newExpense, setNewExpense] = useState({ amount: '', name: '', description: '', category: '', date: '' });
  const [error, setError] = useState('');
  const navigate = useNavigate();
  const { t } = useTranslate();

  useEffect(() => {
    const fetchExpenses = async () => {
      try {
        const response = await axios.get(`${import.meta.env.VITE_API_BASE_URL}/api/expenses`, {
          headers: { Authorization: localStorage.getItem('token') }
        });
        setExpenses(response.data);
      } catch (error) {
        console.error('Failed to fetch expenses', error);
        if (error.response && error.response.status === 401) {
          localStorage.removeItem('token');
          localStorage.removeItem('userId');
          navigate('/login');
        }
      }
    };
    fetchExpenses();
  }, [navigate]);

  const handleInputChange = (e) => {
    setNewExpense({ ...newExpense, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const response = await axios.post(`${import.meta.env.VITE_API_BASE_URL}/api/expenses`, newExpense, {
        headers: { Authorization: localStorage.getItem('token') }
      });
      setExpenses([response.data, ...expenses]);
      setNewExpense({ amount: '', name: '', description: '', category: '', date: '' });
      setError('');
    } catch (error) {
      console.error('Failed to add expense', error);
      setError('Failed to add expense. Please try again.');
      if (error.response && error.response.status === 401) {
        localStorage.removeItem('token');
        localStorage.removeItem('userId');
        navigate('/login');
      }
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
    navigate('/login');
  };

  return (
    <div className="container mx-auto px-4 py-8">
      <LangSelector />
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold">{t('expense_tracker', 'Expense Tracker')}</h1>
        
        <button onClick={handleLogout} className="bg-red-500 hover:bg-red-700 text-white font-bold py-2 px-4 rounded">
          {t('logout', 'Logout')}
        </button>
      </div>
      <div className="bg-white shadow-md rounded px-8 pt-6 pb-8 mb-4">
        <h2 className="text-2xl mb-4">{t('addNewExpense', 'Add New Expense')}</h2>
        <form onSubmit={handleSubmit} className="mb-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <input type="number" name="amount" value={newExpense.amount} onChange={handleInputChange} placeholder={t('amount', 'Amount')} required
              className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline" />
            <input type="text" name="name" value={newExpense.name} onChange={handleInputChange} placeholder={t('name', 'Name')} required
              className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline" />
            <input type="text" name="description" value={newExpense.description} onChange={handleInputChange} placeholder={t('description', 'Description')}
              className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline" />
            <input type="text" name="category" value={newExpense.category} onChange={handleInputChange} placeholder={t('category', 'Category')}
              className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline" />
            <input type="date" name="date" value={newExpense.date} onChange={handleInputChange} required
              className="shadow appearance-none border rounded w-full py-2 px-3 text-gray-700 leading-tight focus:outline-none focus:shadow-outline" />
          </div>
          <button type="submit" className="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded mt-4">
            {t('addExpense', 'Add Expense')}
          </button>
        </form>
        {error && <p className="text-red-500 text-sm mt-2">{t('failedToAddExpense', 'Failed to add expense. Please try again.')}</p>}
      </div>
      <div className="bg-white shadow-md rounded px-8 pt-6 pb-8">
        <h2 className="text-2xl mb-4">{t('expenseList', 'Expense List')}</h2>
        <ul className="divide-y divide-gray-200">
          {expenses.map((expense) => (
            <li key={expense._id} className="py-4">
              <div className="flex justify-between">
                <div>
                  <p className="text-lg font-semibold">{expense.name}</p>
                  <p className="text-gray-600">{expense.description}</p>
                  <p className="text-sm text-gray-500">{expense.category} - {new Date(expense.date).toLocaleDateString()}</p>
                </div>
                <p className="text-lg font-bold">${expense.amount}</p>
              </div>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
}

export default Dashboard;