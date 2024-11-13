// 'use client';
// import { useState } from 'react';
// import { AlertCircle, DollarSign, Tags, Calendar, Moon, Sun, Plus } from 'lucide-react';
// import { useTranslate } from '@tolgee/react';

// export default function ExpenseTracker() {
//   const { t } = useTranslate();
//   const [darkMode, setDarkMode] = useState(false);
//   const [showAddExpenseModal, setShowAddExpenseModal] = useState(false);
//   const [newExpense, setNewExpense] = useState({ category: '', amount: '', note: '', date: '' });
//   const [expenses, setExpenses] = useState([
//     { category: 'Rent', amount: 800, note: 'Monthly apartment rent', date: '2024-04-01' },
//     { category: 'Utilities', amount: 150, note: 'Electricity and water bill', date: '2024-04-05' },
//     { category: 'Dining Out', amount: 60, note: 'Dinner with friends', date: '2024-04-07' },
//   ]);

//   const totalExpenses = expenses.reduce((sum, expense) => sum + expense.amount, 0);

//   const handleAddExpense = () => {
//     if (newExpense.category && newExpense.amount) {
//       setExpenses([
//         ...expenses,
//         {
//           category: newExpense.category,
//           amount: parseFloat(newExpense.amount),
//           note: newExpense.note,
//           date: newExpense.date || new Date().toISOString().split('T')[0],
//         },
//       ]);
//       setShowAddExpenseModal(false);
//       setNewExpense({ category: '', amount: '', note: '', date: '' });
//     }
//   };

//   return (
//     <div className={`min-h-screen ${darkMode ? 'bg-gray-900' : 'bg-gradient-to-br from-blue-50 to-teal-50'}`}>
//       <div className="container mx-auto px-4 py-8">
//         {/* Header Section */}
//         <div className="flex justify-between items-center mb-8">
//           <h1 className={`text-5xl font-extrabold ${darkMode ? 'text-white' : 'text-teal-900'} mb-8`}>
//             {t('expenseTrackerTitle')}
//           </h1>
//           <button
//             onClick={() => setDarkMode(!darkMode)}
//             className={`p-2 rounded-full ${darkMode ? 'bg-gray-800 text-white' : 'bg-white text-gray-800'} shadow-lg`}
//           >
//             {darkMode ? <Sun className="w-5 h-5" /> : <Moon className="w-5 h-5" />}
//           </button>
//         </div>

//         {/* Summary Card */}
//         <div className={`mb-8 p-6 rounded-xl shadow-lg ${darkMode ? 'bg-gray-800' : 'bg-white'}`}>
//           <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
//             <div className={`p-4 rounded-lg ${darkMode ? 'bg-gray-700' : 'bg-blue-50'}`}>
//               <p className={`text-sm ${darkMode ? 'text-gray-400' : 'text-gray-600'}`}>{t('amountLabel')}</p>
//               <p className={`text-2xl font-bold ${darkMode ? 'text-white' : 'text-gray-800'}`}>${totalExpenses}</p>
//             </div>
//             {/* Additional summary items can be added here */}
//           </div>
//         </div>

//         {/* Expenses List in Scrollable View */}
//         <div className="mb-8 space-y-4 overflow-y-scroll h-64">
//           {expenses.map((expense, index) => (
//             <div key={index} className={`p-6 rounded-xl shadow-lg ${darkMode ? 'bg-gray-800' : 'bg-white'}`}>
//               <div className="flex items-center justify-between mb-2">
//                 <div className={`text-lg font-semibold ${darkMode ? 'text-white' : 'text-gray-800'}`}>
//                   {expense.category}
//                 </div>
//                 <div className={`text-2xl font-bold ${darkMode ? 'text-green-400' : 'text-green-600'}`}>
//                   ${expense.amount}
//                 </div>
//               </div>
//               <div className={`text-sm ${darkMode ? 'text-gray-400' : 'text-gray-600'}`}>{expense.note}</div>
//               <div className={`text-sm mt-2 ${darkMode ? 'text-gray-400' : 'text-gray-600'}`}>
//                 {new Date(expense.date).toLocaleDateString()}
//               </div>
//             </div>
//           ))}
//         </div>

//         {/* Add Expense Button */}
//         <button
//           onClick={() => setShowAddExpenseModal(true)}
//           className={`px-4 py-2 rounded-lg flex items-center gap-2 ${
//             darkMode ? 'bg-blue-600 hover:bg-blue-700' : 'bg-blue-500 hover:bg-blue-600'
//           } text-white transition-colors`}
//         >
//           <Plus className="w-4 h-4" />
//           {t('addExpenseButton')}
//         </button>

//         {/* Add Expense Modal */}
//         {showAddExpenseModal && (
//           <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4">
//             <div className={`max-w-md w-full p-6 rounded-xl shadow-xl ${darkMode ? 'bg-gray-800' : 'bg-white'}`}>
//               <h2 className={`text-2xl font-bold mb-6 ${darkMode ? 'text-white' : 'text-gray-800'}`}>
//                 {t('addExpenseTitle')}
//               </h2>
//               <div className="space-y-4">
//                 <div>
//                   <div className="relative">
//                     <Tags className={`absolute left-3 top-3 w-5 h-5 ${darkMode ? 'text-gray-400' : 'text-gray-500'}`} />
//                     <input
//                       type="text"
//                       placeholder={t('categoryPlaceholder')}
//                       value={newExpense.category}
//                       onChange={(e) => setNewExpense({ ...newExpense, category: e.target.value })}
//                       className={`w-full pl-10 pr-4 py-2 rounded-lg ${darkMode ? 'bg-gray-700 text-white' : 'bg-gray-50 text-gray-800'}`}
//                     />
//                   </div>
//                 </div>
//                 <div>
//                   <div className="relative">
//                     <DollarSign className={`absolute left-3 top-3 w-5 h-5 ${darkMode ? 'text-gray-400' : 'text-gray-500'}`} />
//                     <input
//                       type="number"
//                       placeholder={t('amountPlaceholder')}
//                       value={newExpense.amount}
//                       onChange={(e) => setNewExpense({ ...newExpense, amount: e.target.value })}
//                       className={`w-full pl-10 pr-4 py-2 rounded-lg ${darkMode ? 'bg-gray-700 text-white' : 'bg-gray-50 text-gray-800'}`}
//                     />
//                   </div>
//                 </div>
//                 <div>
//                   <div className="relative">
//                     <AlertCircle className={`absolute left-3 top-3 w-5 h-5 ${darkMode ? 'text-gray-400' : 'text-gray-500'}`} />
//                     <input
//                       type="text"
//                       placeholder={t('notePlaceholder')}
//                       value={newExpense.note}
//                       onChange={(e) => setNewExpense({ ...newExpense, note: e.target.value })}
//                       className={`w-full pl-10 pr-4 py-2 rounded-lg ${darkMode ? 'bg-gray-700 text-white' : 'bg-gray-50 text-gray-800'}`}
//                     />
//                   </div>
//                 </div>
//                 <div>
//                   <div className="relative">
//                     <Calendar className={`absolute left-3 top-3 w-5 h-5 ${darkMode ? 'text-gray-400' : 'text-gray-500'}`} />
//                     <input
//                       type="date"
//                       value={newExpense.date}
//                       onChange={(e) => setNewExpense({ ...newExpense, date: e.target.value })}
//                       className={`w-full pl-10 pr-4 py-2 rounded-lg ${darkMode ? 'bg-gray-700 text-white' : 'bg-gray-50 text-gray-800'}`}
//                     />
//                   </div>
//                 </div>
//                 <div className="flex gap-4 mt-6">
//                   <button
//                     onClick={handleAddExpense}
//                     className="flex-1 px-4 py-2 rounded-lg bg-blue-500 text-white hover:bg-blue-600 transition-colors"
//                   >
//                     {t('saveButton')}
//                   </button>
//                   <button
//                     onClick={() => setShowAddExpenseModal(false)}
//                     className="flex-1 px-4 py-2 rounded-lg bg-gray-300 text-gray-800 hover:bg-gray-400 transition-colors"
//                   >
//                     {t('cancelButton')}
//                   </button>
//                 </div>
//               </div>
//             </div>
//           </div>
//         )}
//       </div>
//     </div>
//   );
// }


'use client';
import { useState } from 'react';
import { DollarSign, Moon, Sun, Plus } from 'lucide-react';
import { useTranslate } from '@tolgee/react';

type Expense = {
  category: string;
  amount: number;
  note: string;
  date: string;
};

export default function ExpenseTracker() {
  const { t } = useTranslate();
  const [darkMode, setDarkMode] = useState(false);
  const [showAddExpenseModal, setShowAddExpenseModal] = useState(false);
  const [newExpense, setNewExpense] = useState({ category: '', amount: '', note: '', date: '' });
  const [expenses, setExpenses] = useState<Expense[]>([
    { category: 'Rent', amount: 800, note: 'Monthly apartment rent', date: '2024-04-01' },
    { category: 'Utilities', amount: 150, note: 'Electricity and water bill', date: '2024-04-05' },
    { category: 'Dining Out', amount: 60, note: 'Dinner with friends', date: '2024-04-07' },
  ]);

  const totalExpenses = expenses.reduce((sum, expense) => sum + expense.amount, 0);

  const handleAddExpense = () => {
    if (newExpense.category && newExpense.amount) {
      setExpenses([
        ...expenses,
        {
          category: newExpense.category,
          amount: parseFloat(newExpense.amount),
          note: newExpense.note,
          date: newExpense.date || new Date().toISOString().split('T')[0],
        },
      ]);
      setShowAddExpenseModal(false);
      setNewExpense({ category: '', amount: '', note: '', date: '' });
    }
  };

  // Calculate the percentage of each category
  const categoryPercentages = expenses.reduce((acc, expense) => {
    acc[expense.category] = (acc[expense.category] || 0) + expense.amount;
    return acc;
  }, {} as { [key: string]: number });

  Object.keys(categoryPercentages).forEach(category => {
    categoryPercentages[category] = (categoryPercentages[category] / totalExpenses) * 100;
  });

  // Custom pie chart component
  const PieChart = () => {
    let cumulativePercentage = 0;

    return (
      <svg width="300" height="300" viewBox="-1 -1 2 2" style={{ transform: 'rotate(-90deg)' }}>
        {Object.entries(categoryPercentages).map(([category, percentage], index) => {
          const [startX, startY] = [Math.cos(2 * Math.PI * cumulativePercentage), Math.sin(2 * Math.PI * cumulativePercentage)];
          cumulativePercentage += percentage / 100;
          const [endX, endY] = [Math.cos(2 * Math.PI * cumulativePercentage), Math.sin(2 * Math.PI * cumulativePercentage)];
          const largeArcFlag = percentage > 50 ? 1 : 0;

          // Calculate label position (middle of each arc)
          const labelX = Math.cos(2 * Math.PI * (cumulativePercentage - percentage / 200)) * 0.7;
          const labelY = Math.sin(2 * Math.PI * (cumulativePercentage - percentage / 200)) * 0.7;

          return (
            <g key={index}>
              <path
                d={`M ${startX} ${startY} A 1 1 0 ${largeArcFlag} 1 ${endX} ${endY} L 0 0`}
                fill={`hsl(${index * 50}, 70%, 50%)`}
              >
                <title>{`${category}: ${percentage.toFixed(1)}%`}</title>
              </path>
              <text
                x={labelX}
                y={labelY}
                fill="white"
                fontSize="0.1"
                textAnchor="middle"
                alignmentBaseline="middle"
              >
                {category}
              </text>
            </g>
          );
        })}
      </svg>
    );
  };

  return (
    <div className={`min-h-screen ${darkMode ? 'bg-gray-900' : 'bg-gradient-to-br from-blue-50 to-teal-50'}`}>
      <div className="container mx-auto px-4 py-8">
        {/* Header Section */}
        <div className="flex justify-between items-center mb-8">
          <h1 className={`text-5xl font-extrabold ${darkMode ? 'text-white' : 'text-[black]'} mb-8`}>
            {t('expenseTrackerTitle')}
          </h1>
          <button
            onClick={() => setDarkMode(!darkMode)}
            className={`p-2 rounded-full ${darkMode ? 'bg-gray-800 text-white' : 'bg-white text-gray-800'} shadow-lg`}
          >
            {darkMode ? <Sun className="w-5 h-5" /> : <Moon className="w-5 h-5" />}
          </button>
        </div>

        {/* Total Expenses Display */}
        <div className="flex justify-center gap-[10vw] items-center mb-8">
          <div className={`text-2xl font-bold ${darkMode ? 'text-[white]':  'text-white}'}`}>
            Total Expenses: ${totalExpenses.toFixed(2)}
          </div>
          <div className="flex justify-center mb-8">
            <div className="w-80 h-80">
              <PieChart />
            </div>
          </div>
        </div>

        {/* Expense List */}
        <div className="space-y-4">
          {expenses.map((expense, index) => (
            <div
              key={index}
              className={`p-4 rounded-lg shadow-md flex items-center justify-between ${
                darkMode ? 'bg-gray-800 text-white' : 'bg-white text-gray-800'
              }`}
            >
              <div className="flex items-center space-x-4">
                <DollarSign className="w-6 h-6" />
                <div>
                  <h3 className="font-bold">{expense.category}</h3>
                  <p className="text-sm">{expense.note}</p>
                </div>
              </div>
              <div className="text-right">
                <p className="font-bold">${expense.amount.toFixed(2)}</p>
                <p className="text-xs">{expense.date}</p>
              </div>
            </div>
          ))}
        </div>

        {/* Add Expense Button */}
        <div className="mt-8 flex justify-center">
          <button
            onClick={() => setShowAddExpenseModal(true)}
            className="p-3 rounded-full flex gap-2 bg-teal-600 text-white shadow-lg hover:bg-teal-700"
          >
            <Plus className="w-6 h-6" />
            {t('addExpenseButton')}
          </button>
        </div>

        {/* Add Expense Modal */}
        {showAddExpenseModal && (
          <div className="fixed inset-0 flex items-center justify-center bg-black bg-opacity-50">
            <div className={`p-6 rounded-lg shadow-md ${darkMode ? 'bg-gray-800 text-white' : 'bg-white text-gray-800'}`}>
              <h2 className="text-lg font-bold mb-4">Add New Expense</h2>
              <div className="space-y-4">
                <input
                  type="text"
                  placeholder={t('categoryPlaceholder')}
                  value={newExpense.category}
                  onChange={e => setNewExpense({ ...newExpense, category: e.target.value })}
                  className="w-full p-2 rounded border"
                />
                <input
                  type="number"
                  placeholder={t('amountPlaceholder')}
                  value={newExpense.amount}
                  onChange={e => setNewExpense({ ...newExpense, amount: e.target.value })}
                  className="w-full p-2 rounded border"
                />
                <input
                  type="text"
                  placeholder={t('notePlaceholder')}
                  value={newExpense.note}
                  onChange={e => setNewExpense({ ...newExpense, note: e.target.value })}
                  className="w-full p-2 rounded border"
                />
                <input
                  type="date"
                  value={newExpense.date}
                  onChange={e => setNewExpense({ ...newExpense, date: e.target.value })}
                  className="w-full p-2 rounded border"
                />
              </div>
              <div className="flex justify-end space-x-4 mt-6">
                <button onClick={() => setShowAddExpenseModal(false)} className="px-4 py-2 rounded bg-gray-400 text-white">
                  Cancel
                </button>
                <button onClick={handleAddExpense} className="px-4 py-2 rounded bg-teal-600 text-white">
                  Add Expense
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
