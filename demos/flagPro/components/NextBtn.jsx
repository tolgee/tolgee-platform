import { View, Text, Pressable } from "react-native"
import React from "react"
import { useRouter } from "expo-router"
import questions from "../constants/questions"
import { T } from "@tolgee/react"

const NextBtn = ({
	quesId,
	setQuesId,
	quesResultChecks,
	setQuesResultChecks,
	score,
}) => {
	const router = useRouter()
	const max = questions.length - 1
	const handlePress = () => {
		setQuesResultChecks((prev) => ({
			...prev,
			isPressed: "",
			isDisable: false,
		}))
		if (quesId == max) {
			router.replace({
				pathname: `/results`,
				params: { score },
			})
		} else {
			setQuesId((prev) => prev + 1)
		}
	}
	return (
		<View className="z-20 w-full">
			<Pressable
				className={`w-full bg-white p-4 rounded-full ${
					!quesResultChecks.isDisable ? "opacity-30" : "opacity-100"
				}`}
				onPress={handlePress}
				disabled={!quesResultChecks.isDisable}
			>
				<Text
					className="text-center text-2xl text-grad"
					style={{ fontFamily: "CrimsonText_700Bold" }}
				>
					<T keyName="Next" />
				</Text>
			</Pressable>
		</View>
	)
}

export default NextBtn
