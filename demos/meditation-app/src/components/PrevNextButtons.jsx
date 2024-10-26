import { IoIosArrowDropright, IoIosArrowDropleft } from "react-icons/io";

function PrevNextButtons({
  prevButtonText,
  prevButtonOnClick,
  prevButtonDisabled,
  nextButtonText,
  nextButtonOnClick,
  nextButtonDisabled,
}) {
  return (
    <div className={`flex max-[420px]:flex-col gap-4 ${prevButtonText ? "justify-between" : "justify-end"}`}>
      {prevButtonText && (
        <button
          className="btn primary-btn flex gap-2 items-center justify-center"
          onClick={prevButtonOnClick}
          disabled={prevButtonDisabled}
        >
          <IoIosArrowDropleft />
          <span>{prevButtonText}</span>
        </button>
      )}
      {nextButtonText && (
        <button
          className="btn primary-btn flex gap-2 items-center justify-center"
          onClick={nextButtonOnClick}
          disabled={nextButtonDisabled}
        >
          <span>{nextButtonText}</span>
          <IoIosArrowDropright />
        </button>
      )}
    </div>
  );
}

export default PrevNextButtons;
