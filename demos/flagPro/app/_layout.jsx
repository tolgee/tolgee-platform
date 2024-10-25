import { Slot } from "expo-router"
import { SafeAreaView } from "react-native-safe-area-context"
import BgLayout from "../components/BgLayout"
import "../index.css"
import en from "../i18n/en.json"
import it from "../i18n/it-IT.json"
import ja from "../i18n/ja-JP.json"
import ko from "../i18n/ko-KR.json"
import {
	Tolgee,
	DevTools,
	TolgeeProvider,
	FormatSimple,
	T,
} from "@tolgee/react"
import { StatusBar, Text } from "react-native"

export default function RootLayout() {
	const tolgee = Tolgee().use(DevTools()).use(FormatSimple()).init({
		language: "it",

		// // for development
		apiUrl: process.env.EXPO_PUBLIC_TOLGEE_API_URL,
		apiKey: process.env.EXPO_PUBLIC_TOLGEE_API_KEY,
		staticData: { en, it, ja, ko },
	})
	return (
		<SafeAreaView className="relative">
			<TolgeeProvider
				tolgee={tolgee}
				fallback={
					<Text>
						<T keyName="Loading" />
						...
					</Text>
				} // loading fallback
			>
				<BgLayout>
					<StatusBar
						animated={true}
						backgroundColor="#61dafb"
						barStyle="dark-content"
						showHideTransition="fade"
						hidden={true}
					/>
					<Slot />
				</BgLayout>
			</TolgeeProvider>
		</SafeAreaView>
	)
}
