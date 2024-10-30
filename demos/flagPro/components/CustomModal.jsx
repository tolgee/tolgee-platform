import {
	View,
	Text,
	Image,
	Pressable,
	StyleSheet,
	TouchableOpacity,
} from "react-native"
import React from "react"
import langs from "../constants/langs"
import { T, useTolgee } from "@tolgee/react"
import { Defs, LinearGradient, Path, Rect, Stop, Svg } from "react-native-svg"

const CustomModal = ({ setOpenModal }) => {
	console.log("here")
	const tolgee = useTolgee(["language"])
	console.log("tolgee", tolgee.getLanguage())
	const selectedLang = tolgee.getLanguage()
	return (
		<>
			<View className="w-5/6 absolute   ">
				<Pressable
					className="w-fit h-fit rounded-full bg-white border-white border-[1px] absolute z-[55] -right-4 -top-3 p-1"
					onPress={() => setOpenModal(false)}
				>
					<Text>
						<Svg
							xmlns="http://www.w3.org/2000/svg"
							width={24}
							height={24}
							fill="none"
							id="close"
						>
							<Path
								fill="#000"
								d="M7.05 7.05a1 1 0 0 0 0 1.414L10.586 12 7.05 15.536a1 1 0 1 0 1.414 1.414L12 13.414l3.536 3.536a1 1 0 0 0 1.414-1.414L13.414 12l3.536-3.536a1 1 0 0 0-1.414-1.414L12 10.586 8.464 7.05a1 1 0 0 0-1.414 0Z"
							/>
						</Svg>
					</Text>
				</Pressable>
				<View className="overflow-hidden h-fit items-center flex py-6 w-full border-[1px] border-white rounded-lg">
					<Svg width="100%" style={StyleSheet.absoluteFill}>
						<Defs>
							<LinearGradient id="grad" x1="0%" y1="100%" x2="100%" y2="0%">
								<Stop offset="0" stopColor="rgb(236, 64, 199)" />
								<Stop offset="1" stopColor="rgb(0, 0, 0)" />
							</LinearGradient>
						</Defs>
						<Rect width="100%" height="100%" fill="url(#grad)" />
					</Svg>
					<Text
						className="text-xl text-center -mt-6 py-4   text-white font-semibold "
						style={{ fontFamily: "CrimsonText_600SemiBold" }}
					>
						<T keyName="Select Language" />
					</Text>
					{langs.map((lang) => (
						<TouchableOpacity
							activeOpacity={0.8}
							className={`flex flex-row p-2 w-4/6 my-2 rounded-full items-center justify-center bg-white/70 
                                ${
																	selectedLang == lang.language
																		? "border-[1px] border-white text-white bg-black/40"
																		: ""
																}
                            `}
							key={lang.language}
							onPress={(e) => {
								console.log(lang.language)
								tolgee.changeLanguage(lang.language)
							}}
						>
							<>
								{selectedLang == lang.language ? (
									<Svg
										xmlns="http://www.w3.org/2000/svg"
										viewBox="0 0 24 24"
										fill="white"
										className="size-4 w-5 h-5 font-semibold mr-3 border-white border-2 "
									>
										<Path
											fillRule="evenodd"
											d="M19.916 4.626a.75.75 0 0 1 .208 1.04l-9 13.5a.75.75 0 0 1-1.154.114l-6-6a.75.75 0 0 1 1.06-1.06l5.353 5.353 8.493-12.74a.75.75 0 0 1 1.04-.207Z"
											clipRule="evenodd"
										/>
									</Svg>
								) : null}
								<Text
									className={`text-lg mr-2  ${
										selectedLang == lang.language ? " text-white" : "text-black"
									}`}
									style={{ fontFamily: "CrimsonText_400Regular" }}
								>
									<T keyName={lang.label} />
								</Text>
								<Image source={lang.flag} className="w-6 h-6" />
							</>
						</TouchableOpacity>
					))}
				</View>
			</View>
		</>
	)
}

export default CustomModal
