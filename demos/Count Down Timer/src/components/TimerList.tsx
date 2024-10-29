// src/components/TimerList.tsx
import { useEffect } from 'react';
import Countdown from './Countdown';

interface Timer {
  event: string;
  targetDate: string;
}

interface TimerListProps {
  timers: Timer[];
  removeTimer: (index: number) => void;
}

const TimerList: React.FC<TimerListProps> = ({ timers, removeTimer }) => {
   useEffect(() => {
    if (Notification.permission !== 'granted') {
      Notification.requestPermission();
    }
  }, []);

  const handleTimeUp = (index: number) => {
    if (Notification.permission === 'granted') {
      new Notification('Time Up!', {
        body: `The countdown for ${timers[index].event} is over!`,
      });
    }
    removeTimer(index);
  };

  return (
    <div className="space-y-4">
      {timers.map((timer, index) => (
        <Countdown
          key={index}
          event={timer.event}
          targetDate={timer.targetDate}
          onTimeUp={() => handleTimeUp(index)}
          onRemove={() => removeTimer(index)}  
        />
      ))}
    </div>
  );
};

export default TimerList;
