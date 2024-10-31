'use client';
import { useState } from 'react';
import { AlertCircle, ArrowLeft, ArrowRight, BookmarkPlus, List, Moon, Sun, Plus, DollarSign, Tags, Calendar } from 'lucide-react';
import { useTranslate } from '@tolgee/react';

export default function ExpenseTracker() {
  const { t } = useTranslate();
  const [currentExpense, setCurrentExpense] = useState(0);
  const [animate, setAnimate] = useState(false);
  const [bookmark, setBookmark] = useState<number | null>(null);
  const [showTOC, setShowTOC] = useState(false);
  const [darkMode, setDarkMode] = useState(false);
  const [showAddExpenseModal, setShowAddExpenseModal] = useState(false);
  const [newExpense, setNewExpense] = useState({ category: '', amount: '', note: '', date: '' });
  const [expenses, setExpenses] = useState([
    { category: 'Groceries', amount: 50, note: 'Weekly groceries', date: '2024-03-28' },
    { category: 'Transportation', amount: 20, note: 'Bus fare', date: '2024-03-29' },
    { category: 'Entertainment', amount: 30, note: 'Movie night', date: '2024-03-30' },
  ]);

  const totalExpenses = expenses.reduce((sum, expense) => sum + expense.amount, 0);

  const handleExpenseChange = (newIndex: number) => {
    setAnimate(true);
    setTimeout(() => {
      setCurrentExpense(newIndex);
      setAnimate(false);
    }, 300);
  };

  const handleAddExpense = () => {
    if (newExpense.category && newExpense.amount) {
      setExpenses([...expenses, { 
        category: newExpense.category, 
        amount: parseFloat(newExpense.amount), 
        note: newExpense.note,
        date: newExpense.date || new Date().toISOString().split('T')[0]
      }]);
      setShowAddExpenseModal(false);
      setNewExpense({ category: '', amount: '', note: '', date: '' });
    }
  };

  return (
    <div className={`min-h-screen ${darkMode ? 'bg-gray-900' : 'bg-gradient-to-br from-blue-50 to-teal-50'}`}>
      <div className="container mx-auto px-4 py-8">
        {/* Header Section */}
        <div className="flex justify-between items-center mb-8">
        <h1 className={`text-5xl font-extrabold text-center ${darkMode ? 'text-white' : 'text-teal-900'} mb-8 tracking-wide shadow-lg transition duration-700 ease-in-out transform hover:rotate-1 hover:scale-105`}>
          {t('expenseTrackerTitle')}
        </h1>

          <button
            onClick={() => setDarkMode(!darkMode)}
            className={`p-2 rounded-full ${darkMode ? 'bg-gray-800 text-white' : 'bg-white text-gray-800'} shadow-lg`}
          >
            {darkMode ? <Sun className="w-5 h-5" /> : <Moon className="w-5 h-5" />}
          </button>
        </div>

        {/* Summary Card */}
        <div className={`mb-8 p-6 rounded-xl shadow-lg ${darkMode ? 'bg-gray-800' : 'bg-white'}`}>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div className={`p-4 rounded-lg ${darkMode ? 'bg-gray-700' : 'bg-blue-50'}`}>
              <p className={`text-sm ${darkMode ? 'text-gray-400' : 'text-gray-600'}`}>{t('amountLabel')}</p>
              <p className={`text-2xl font-bold ${darkMode ? 'text-white' : 'text-gray-800'}`}>
                ${totalExpenses}
              </p>
            </div>
            <div className={`p-4 rounded-lg ${darkMode ? 'bg-gray-700' : 'bg-green-50'}`}>
              <p className={`text-sm ${darkMode ? 'text-gray-400' : 'text-gray-600'}`}>{t('categoryLabel')}</p>
              <p className={`text-2xl font-bold ${darkMode ? 'text-white' : 'text-gray-800'}`}>
                {new Set(expenses.map(e => e.category)).size}
              </p>
            </div>
            <div className={`p-4 rounded-lg ${darkMode ? 'bg-gray-700' : 'bg-purple-50'}`}>
              <p className={`text-sm ${darkMode ? 'text-gray-400' : 'text-gray-600'}`}>{t('noteLabel')}</p>
              <p className={`text-2xl font-bold ${darkMode ? 'text-white' : 'text-gray-800'}`}>
                ${expenses.reduce((sum, e) => {
                  const date = new Date(e.date);
                  const now = new Date();
                  return date.getMonth() === now.getMonth() ? sum + e.amount : sum;
                }, 0)}
              </p>
            </div>
            <div className={`p-4 rounded-lg ${darkMode ? 'bg-gray-700' : 'bg-orange-50'}`}>
              <p className={`text-sm ${darkMode ? 'text-gray-400' : 'text-gray-600'}`}>{t('average')}</p>
              <p className={`text-2xl font-bold ${darkMode ? 'text-white' : 'text-gray-800'}`}>
                ${(totalExpenses / expenses.length).toFixed(2)}
              </p>
            </div>
          </div>
        </div>

        {/* Current Expense Card */}
        <div className={`mb-8 p-6 rounded-xl shadow-lg ${darkMode ? 'bg-gray-800' : 'bg-white'}`}>
          <div className={`transition-all duration-300 ${animate ? 'opacity-0 transform -translate-y-4' : 'opacity-100'}`}>
            <div className="flex items-center justify-between mb-4">
              <div className={`text-lg font-semibold ${darkMode ? 'text-white' : 'text-gray-800'}`}>
                {expenses[currentExpense].category}
              </div>
              <div className={`text-2xl font-bold ${darkMode ? 'text-green-400' : 'text-green-600'}`}>
                ${expenses[currentExpense].amount}
              </div>
            </div>
            <div className={`text-sm ${darkMode ? 'text-gray-400' : 'text-gray-600'}`}>
              {expenses[currentExpense].note}
            </div>
            <div className={`text-sm mt-2 ${darkMode ? 'text-gray-400' : 'text-gray-600'}`}>
              {new Date(expenses[currentExpense].date).toLocaleDateString()}
            </div>
          </div>

          {/* Navigation Controls */}
          <div className="flex justify-between items-center mt-6">
            <button
              onClick={() => handleExpenseChange(currentExpense - 1)}
              disabled={currentExpense === 0}
              className={`p-2 rounded-lg ${darkMode ? 'bg-gray-700 hover:bg-gray-600' : 'bg-gray-100 hover:bg-gray-200'} 
                disabled:opacity-50 disabled:cursor-not-allowed transition-colors`}
            >
              <ArrowLeft className={`w-5 h-5 ${darkMode ? 'text-white' : 'text-gray-800'}`} />
            </button>
            <button
              onClick={() => setShowAddExpenseModal(true)}
              className={`px-4 py-2 rounded-lg flex items-center gap-2 ${
                darkMode ? 'bg-blue-600 hover:bg-blue-700' : 'bg-blue-500 hover:bg-blue-600'
              } text-white transition-colors`}
            >
              <Plus className="w-4 h-4" />
              {t('addExpenseButton')}
            </button>
            <button
              onClick={() => handleExpenseChange(currentExpense + 1)}
              disabled={currentExpense === expenses.length - 1}
              className={`p-2 rounded-lg ${darkMode ? 'bg-gray-700 hover:bg-gray-600' : 'bg-gray-100 hover:bg-gray-200'}
                disabled:opacity-50 disabled:cursor-not-allowed transition-colors`}
            >
              <ArrowRight className={`w-5 h-5 ${darkMode ? 'text-white' : 'text-gray-800'}`} />
            </button>
          </div>
        </div>

        {/* Add Expense Modal */}
        {showAddExpenseModal && (
          <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4">
            <div className={`max-w-md w-full p-6 rounded-xl shadow-xl ${darkMode ? 'bg-gray-800' : 'bg-white'}`}>
              <h2 className={`text-2xl font-bold mb-6 ${darkMode ? 'text-white' : 'text-gray-800'}`}>
              {t('addExpenseTitle')}
              </h2>
              <div className="space-y-4">
                <div>
                  <div className="relative">
                    <Tags className={`absolute left-3 top-3 w-5 h-5 ${darkMode ? 'text-gray-400' : 'text-gray-500'}`} />
                    <input
                      type="text"
                      placeholder={t('categoryPlaceholder')}
                      value={newExpense.category}
                      onChange={(e) => setNewExpense({ ...newExpense, category: e.target.value })}
                      className={`w-full pl-10 pr-4 py-2 rounded-lg ${
                        darkMode ? 'bg-gray-700 text-white' : 'bg-gray-50 text-gray-800'
                      } border border-gray-300 focus:ring-2 focus:ring-blue-500 focus:border-transparent`}
                    />
                  </div>
                </div>
                <div>
                  <div className="relative">
                    <DollarSign className={`absolute left-3 top-3 w-5 h-5 ${darkMode ? 'text-gray-400' : 'text-gray-500'}`} />
                    <input
                      type="number"
                      placeholder={t('amountPlaceholder')}
                      value={newExpense.amount}
                      onChange={(e) => setNewExpense({ ...newExpense, amount: e.target.value })}
                      className={`w-full pl-10 pr-4 py-2 rounded-lg ${
                        darkMode ? 'bg-gray-700 text-white' : 'bg-gray-50 text-gray-800'
                      } border border-gray-300 focus:ring-2 focus:ring-blue-500 focus:border-transparent`}
                    />
                  </div>
                </div>
                <div>
                  <div className="relative">
                    <AlertCircle className={`absolute left-3 top-3 w-5 h-5 ${darkMode ? 'text-gray-400' : 'text-gray-500'}`} />
                    <input
                      type="text"
                      placeholder={t('notePlaceholder')}
                      value={newExpense.note}
                      onChange={(e) => setNewExpense({ ...newExpense, note: e.target.value })}
                      className={`w-full pl-10 pr-4 py-2 rounded-lg ${
                        darkMode ? 'bg-gray-700 text-white' : 'bg-gray-50 text-gray-800'
                      } border border-gray-300 focus:ring-2 focus:ring-blue-500 focus:border-transparent`}
                    />
                  </div>
                </div>
                <div>
                  <div className="relative">
                    <Calendar className={`absolute left-3 top-3 w-5 h-5 ${darkMode ? 'text-gray-400' : 'text-gray-500'}`} />
                    <input
                      type="date"
                      value={newExpense.date}
                      onChange={(e) => setNewExpense({ ...newExpense, date: e.target.value })}
                      className={`w-full pl-10 pr-4 py-2 rounded-lg ${
                        darkMode ? 'bg-gray-700 text-white' : 'bg-gray-50 text-gray-800'
                      } border border-gray-300 focus:ring-2 focus:ring-blue-500 focus:border-transparent`}
                    />
                  </div>
                </div>
                <div className="flex gap-4 mt-6">
                  <button
                    onClick={handleAddExpense}
                    className="flex-1 px-4 py-2 rounded-lg bg-blue-500 text-white hover:bg-blue-600 transition-colors"
                  >
                    {t('saveButton')}
                  </button>
                  <button
                    onClick={() => setShowAddExpenseModal(false)}
                    className={`flex-1 px-4 py-2 rounded-lg ${
                      darkMode ? 'bg-gray-700 text-white hover:bg-gray-600' : 'bg-gray-100 text-gray-800 hover:bg-gray-200'
                    } transition-colors`}
                  >
                    {t('cancelButton')}
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}