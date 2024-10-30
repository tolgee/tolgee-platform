import { View } from "react-native"
import React, { useCallback } from "react"
import { SplashScreen } from "expo-router"
import RadialGradientComp from "./RadialGradientComp"
import { Rye_400Regular, useFonts } from "@expo-google-fonts/rye"
import {
	CrimsonText_400Regular,
	CrimsonText_600SemiBold,
	CrimsonText_700Bold,
} from "@expo-google-fonts/crimson-text"

const BgLayout = ({ children, classes }) => {
	const [loaded, error] = useFonts({
		Rye_400Regular,
		CrimsonText_400Regular,
		CrimsonText_600SemiBold,
		CrimsonText_700Bold,
	})
	const onLayoutRootView = useCallback(async () => {
		if (loaded) {
			await SplashScreen.hideAsync() // Hide splash screen when fonts are loaded
		}
	}, [loaded])
	if (!loaded) {
		return null // Return null while fonts are loading
	}
	return (
		<View
			className={`w-full h-full ${classes ? classes : ""}`}
			onLayout={onLayoutRootView}
		>
			<View className="relative h-full bg-black -z-10 w-full">
				<RadialGradientComp classes="-left-16 top-2" />
				<RadialGradientComp classes="left-0 top-24" />
				<RadialGradientComp classes="-right-20 top-44" />
				<RadialGradientComp classes="-left-20 top-[60%]" />
				<RadialGradientComp classes="-right-14 top-3/4" />
			</View>
			{children}
		</View>
	)
}

export default BgLayout
