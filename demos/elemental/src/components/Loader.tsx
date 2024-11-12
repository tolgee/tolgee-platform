// import React from 'react'

const RippleLoader = () => {
  return (
    <div className="lds-ripple relative inline-block w-20 h-20">
      <div className="absolute border-4 border-current rounded-full opacity-100 animate-ripple"></div>
      <div className="absolute border-4 border-current rounded-full opacity-100 animate-ripple delay-100"></div>
      
      <style>{`
        .lds-ripple div {
          animation: lds-ripple 1s cubic-bezier(0, 0.2, 0.8, 1) infinite;
        }

        .lds-ripple div:nth-child(2) {
          animation-delay: -0.5s;
        }

        @keyframes lds-ripple {
          0% {
            top: 36px;
            left: 36px;
            width: 8px;
            height: 8px;
            opacity: 0;
          }
          4.9% {
            top: 36px;
            left: 36px;
            width: 8px;
            height: 8px;
            opacity: 0;
          }
          5% {
            top: 36px;
            left: 36px;
            width: 8px;
            height: 8px;
            opacity: 1;
          }
          100% {
            top: 0;
            left: 0;
            width: 80px;
            height: 80px;
            opacity: 0;
          }
        }
      `}</style>
    </div>
  );
};

export default RippleLoader;
