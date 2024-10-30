import { View, Text, Pressable } from "react-native"
import React from "react"
import { useLocalSearchParams } from "expo-router"
import Trophy from "../components/Trophy"
import questions from "../constants/questions"
import { useRouter } from "expo-router"
import { T } from "@tolgee/react"

const Results = () => {
	const { score } = useLocalSearchParams()
	const router = useRouter()
	return (
		<View className="w-screen h-full z-20 absolute left-0 top-0  pt-10 flex items-center justify-around">
			<View className="mt-4 flex items-center">
				<Text
					className="text-white text-4xl z-20"
					style={{ fontFamily: "CrimsonText_600SemiBold" }}
				>
					<T keyName="Congrats" />!
				</Text>
				<Text
					className="text-white text-lg z-20"
					style={{ fontFamily: "CrimsonText_400Regular" }}
				>
					<T keyName="You have completed the quiz" />!
				</Text>
			</View>

			<Trophy />

			<Text
				className="text-white text-4xl z-20"
				style={{ fontFamily: "CrimsonText_600SemiBold" }}
			>
				<T keyName="You scored" /> {score ?? 0}/{questions.length}
			</Text>

			<View className="flex items-center z-20 w-[90%]">
				<Pressable
					className={`w-full bg-white p-4 rounded-full `}
					onPress={() => router.replace("/")}
				>
					<Text
						className="text-center font-semibold text-2xl text-grad"
						style={{ fontFamily: "CrimsonText_600SemiBold" }}
					>
						<T keyName="Restart" />
					</Text>
				</Pressable>
				{/* )} */}
			</View>
		</View>
	)
}

export default Results
