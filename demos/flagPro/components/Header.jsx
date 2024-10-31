import { View, Text } from "react-native"
import React from "react"
import TranslateBtn from "./Translation/TranslateBtn"

const Header = () => {
	return (
		<View className=" w-full h-20 flex mb-4 justify-between items-center flex-row">
			<View className="logo relative flex justify-center  h-full">
				<Text
					style={{ fontFamily: "Rye_400Regular" }}
					className="text-white z-10 shadow-lg  text-xl "
				>
					FlagPro
				</Text>
			</View>
			<TranslateBtn />
		</View>
	)
}

export default Header
