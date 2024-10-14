import React, { useEffect, useRef, useState } from "react";
import { IconButton } from "@mui/material";
import BlurOnIcon from "@mui/icons-material/BlurOn";
import PlayCircleIcon from "@mui/icons-material/PlayCircle";
import VideoModal from "./VideoModal";
import { useTranslate } from "@tolgee/react";
import RollDice from "./RollDice";
import { Canvas } from "@react-three/fiber";

const GameScreen = () => {
  const { t } = useTranslate();
  const rollDiceRef = useRef();
  const [isChartReady, setIsChartReady] = useState(false);
  const [modalData, setModalData] = useState("");
  const [modalVisible, setModalVisible] = useState(false);
  const exercises = [
    "",
    "push-ups",
    "pull-ups",
    "squats",
    "planks",
    "lunges",
    "burpees",
  ];
  const [filteredItem, setFilteredItem] = useState([]);

  const handleDiceRoll = () => {
    if (filteredItem.length >= 6) {
      setIsChartReady(true);
      return;
    }
    const totalItems = exercises.length - 1;
    const selectedExercise = Math.ceil(Math.random() * totalItems);
    if (
      filteredItem.length &&
      filteredItem.some((item) => item === selectedExercise)
    ) {
      handleDiceRoll();
    } else {
      setFilteredItem((prev) => [...prev, selectedExercise]);
      if (rollDiceRef.current) {
        rollDiceRef.current.startRoll(); // calling the exposed method.
      }
    }
  };

  const handleRestart = () => {
    setFilteredItem([]);
  };
  
  return (
    <section className="game-screen flex flex-col-reverse md:flex-row items-start justify-center w-full gap-11 px-4 md:px-0">
      <article className="box w-full md:w-2/5 min-h-[250px] relative">
        <h1 className="text-base font-semibold dark-text">
          {t("your-challenge", "Your Challenges")}
        </h1>
        <div className="cards-container mt-4 border-t transition-all duration-200 ">
          {filteredItem &&
            filteredItem.map((item, idx) => (
              <article
                key={exercises[item]}
                className="flex items-center justify-between w-full mt-2"
              >
                <div className="flex items-center gap-2">
                  <figure>
                    <BlurOnIcon sx={{ fill: "red" }} />
                  </figure>
                  <div>
                    <p className="text-sm md:text-base capitalize ">
                      {" "}
                      {t(exercises[item], exercises[item])}
                    </p>
                    <p className="text-xs opacity-70">
                      3 x {t("sets", "sets")}
                    </p>
                  </div>
                </div>
                <div
                  onClick={() => {
                    setModalData(exercises[item]);
                    setModalVisible(!modalVisible);
                  }}
                  className="text-xs capitalize primary-color flex items-center h-4 gap-1 cursor-pointer"
                >
                  <PlayCircleIcon sx={{ fill: "#4E8098" }} />{" "}
                  {t("tutorial", "tutorial")}
                </div>
              </article>
            ))}
        </div>
        {filteredItem.length == 0 && (
          <img
            src="/noData.svg"
            className="h-24 w-24 absolute left-1/2 -translate-x-1/2 bottom-10"
          />
        )}
      </article>
      <article className="flex flex-col items-start justify-between gap-3 md:gap-8 w-full md:w-2/5">
        <aside className="box w-full flex flex-col items-center gap-2 relative h-[220px] md:h-[250px]">
          <figure className="h-[150px] w-full">
            <Canvas>
              <RollDice ref={rollDiceRef} result={filteredItem[filteredItem.length-1]} />
            </Canvas>
          </figure>
          <div className="primary-back white-text font-semibold uppercase text-sm md:text-base absolute left-1/2 -translate-x-1/2 bottom-4">
            <IconButton
              onClick={handleDiceRoll}
              sx={{
                fontSize: "inherit",
                color: "white",
                paddingInline: "24px",
                paddingBlock: "12px",
                width: '100%',
                height: '100%',
              }}
              disabled={filteredItem.length == 6}
            >
              {t("roll-dice", "Roll Dice")}
            </IconButton>
          </div>
        </aside>
        <aside className="box w-full flex items-center justify-between gap-2">
          <h3 className="text-xs sm:text-sm lg:text-base font-semibold dark-text capitalize">
            {/* {t("chances-left", "Chances Left")} */}
            {t('dice-rolled','dice rolled')}
          </h3>
          <ul className="flex items-center gap-2 lg:gap-4 text-sm md:text-base font-medium dark-text">
            {6 - filteredItem.length > -1 ? (
              // 6 - filteredItem.length
              filteredItem.map((item, idx)=>(<li key={item}>{item}</li>))
            ) : ( null
            )}
              {filteredItem.length == 6 && <IconButton onClick={handleRestart}>
                <div className="text-sm secondary-color">
                  {t("restart", "Restart")}
                </div>
              </IconButton>}
          </ul>
        </aside>
      </article>

      <VideoModal
        name={modalData}
        modalVisible={modalVisible}
        setModalVisible={setModalVisible}
      />
    </section>
  );
};

export default GameScreen;
