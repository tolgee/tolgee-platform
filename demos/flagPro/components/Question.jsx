import { Text, Pressable } from "react-native"
import questions from "../constants/questions"
import { T } from "@tolgee/react"

const Question = ({
	id,
	option,
	quesResultChecks,
	setQuesResultChecks,
	setScore,
}) => {
	const handleOption = (text) => {
		const ans = questions[id].answer
		setQuesResultChecks((prev) => ({
			...prev,
			isPressed: text,
			isDisable: true,
		}))

		if (text == ans) {
			console.log("point")
			setScore((prev) => prev + 1)
		}
	}

	return (
		<Pressable
			className={`w-full rounded-full p-4 mt-2 border-[1px]
			    ${
						quesResultChecks.isPressed !== "" &&
						quesResultChecks.isPressed !== option
							? "border-transparent opacity-70 bg-white/50"
							: !(quesResultChecks.isPressed == option)
							? "bg-white/40 border-white"
							: quesResultChecks.isPressed == questions[id].answer
							? "bg-green-500/60 border-green-500"
							: "bg-red-500/80 border-red-500"
					}
			`}
			disabled={quesResultChecks.isDisable}
			onPress={() => handleOption(option)}
		>
			<Text
				className={`text-center  text-xl  ${
					quesResultChecks.isPressed !== "" &&
					quesResultChecks.isPressed !== option
						? "text-white/50"
						: "text-white"
				}`}
				style={{ fontFamily: "CrimsonText_600SemiBold" }}
			>
				<T keyName={option} />
			</Text>
		</Pressable>
	)
}

export default Question
