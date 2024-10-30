import { View, Text, Pressable } from "react-native"
import React from "react"
import { useRouter } from "expo-router"
import { T } from "@tolgee/react"

const CustomBtn = ({ text }) => {
	const router = useRouter()

	return (
		<View className="flex items-center bottom-5 z-20 absolute w-full">
			<Pressable
				className=" w-[90%] bg-white p-4 rounded-full"
				onPress={() => router.push("/ques/q0")}
			>
				<Text
					className="text-center text-2xl text-grad"
					style={{ fontFamily: "CrimsonText_700Bold" }}
				>
					<T keyName={text} />
				</Text>
			</Pressable>
		</View>
	)
}

export default CustomBtn
