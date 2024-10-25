import { View, Text, Image } from "react-native"
import React, { useState } from "react"
import questions from "../../constants/questions"
import NextBtn from "../../components/NextBtn"
import Question from "../../components/Question"
import Header from "../../components/Header"
import { T } from "@tolgee/react"

const QuesPage = () => {
	const [quesId, setQuesId] = useState(0)
	const [quesResultChecks, setQuesResultChecks] = useState({
		isPressed: "",
		disable: false,
	})
	const [score, setScore] = useState(0)

	return (
		<View className="p-6 flex justify-between z-20 absolute w-full h-full">
			<Header />
			<View className="flex h-full max-h-[85%] items-center justify-between z-20  w-full">
				<View className="w-full h-44 rounded-lg border-white border-[2px] overflow-hidden">
					<Image
						source={{
							uri: questions[quesId].flag,
						}}
						className="w-full h-full"
					/>
				</View>
				<View className="w-full ">
					<Text
						className="text-2xl mb-2 text-center text-white"
						style={{ fontFamily: "CrimsonText_600SemiBold" }}
					>
						<T keyName="Guess the country name" />
					</Text>
					<View className="w-full">
						{questions[quesId].options.map((option) => (
							<Question
								id={quesId}
								option={option}
								key={option}
								quesResultChecks={quesResultChecks}
								setQuesResultChecks={setQuesResultChecks}
								setScore={setScore}
							/>
						))}
					</View>
				</View>
				<NextBtn
					quesId={quesId}
					setQuesId={setQuesId}
					quesResultChecks={quesResultChecks}
					setQuesResultChecks={setQuesResultChecks}
					score={score}
				/>
			</View>
		</View>
	)
}

export default QuesPage
