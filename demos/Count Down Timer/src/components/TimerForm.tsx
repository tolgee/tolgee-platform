// src/components/TimerForm.tsx
import { useState } from 'react';
import toast from 'react-hot-toast';
import { motion } from 'framer-motion';
import { useTranslate } from '@tolgee/react';

interface TimerFormProps {
  addTimer: (event: string, targetDate: string) => void;
}

const TimerForm: React.FC<TimerFormProps> = ({ addTimer }) => {
  const [event, setEvent] = useState('');
  const [targetDate, setTargetDate] = useState('');
  const [isValid, setIsValid] = useState(false);
  const { t } = useTranslate();

  const handleDateChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const date = new Date(e.target.value).getTime();
    const now = new Date().getTime();

    if (date > now) {
      setTargetDate(e.target.value);
      setIsValid(true);
    } else {
      setIsValid(false);
      toast.error(`${t('future-error')}`);
    }
  };
  

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (event && isValid) {
      addTimer(event, targetDate);
      setEvent('');
      setTargetDate('');
      toast.success(`${t('success')}`);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="mb-8 space-y-4 w-full max-w-md mx-auto">
      <div>
        <label htmlFor="event" className="block text-sm text-gray-400">{t('event-name')}</label>
        <input
          id="event"
          type="text"
          value={event}
          onChange={(e) => setEvent(e.target.value)}
          className="w-full p-2 rounded bg-gray-700 text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
          placeholder={t('enter-event')}
        />
      </div>
      <div>
        <label htmlFor="date" className="block text-sm text-gray-400">{t('target-date-time')}</label>
        <input
          id="date"
          type="datetime-local"
          value={targetDate}
          onChange={handleDateChange}
          className="w-full p-2 rounded bg-gray-700 text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
      </div>
      <motion.button
        whileHover={{ scale: 1.05 }}
        whileTap={{ scale: 0.95 }}
        type="submit"
        className={`w-full p-3 rounded shadow-lg text-white font-bold ${isValid ? 'bg-blue-600' : 'bg-gray-600 cursor-not-allowed'}`}
        disabled={!isValid}
      >
        {t('set-timer')}
      </motion.button>
    </form>
  );
};

export default TimerForm;