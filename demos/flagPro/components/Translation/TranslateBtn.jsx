import { Pressable, View } from "react-native"
import React, { useState } from "react"
import Svg, { Path, Rect, Line, Polyline } from "react-native-svg"
import CustomModal from "../CustomModal"
const TranslateBtn = ({ classes }) => {
	const [openModal, setOpenModal] = useState(false)
	return (
		<>
			<Pressable
				className={`bg-white w-10 h-10 p-2 rounded-md ${
					classes ? classes : ""
				}`}
				onPress={() => {
					console.log("open")
					setOpenModal(true)
				}}
			>
				<Svg
					xmlns="http://www.w3.org/2000/svg"
					viewBox="0 0 256 256"
					id="translate"
				>
					<Rect width={256} height={256} fill="none" className="p-2" />
					<Polyline
						fill="none"
						stroke="#ec40c7"
						strokeLinecap="round"
						strokeLinejoin="round"
						strokeWidth={24}
						points="231.982 216 175.982 104 119.982 216"
					/>
					<Line
						x1={135.982}
						x2={215.982}
						y1={184}
						y2={184}
						fill="none"
						stroke="#ec40c7"
						strokeLinecap="round"
						strokeLinejoin="round"
						strokeWidth={24}
					/>
					<Line
						x1={87.982}
						x2={87.982}
						y1={32}
						y2={56}
						fill="none"
						stroke="#ec40c7"
						strokeLinecap="round"
						strokeLinejoin="round"
						strokeWidth={24}
					/>
					<Line
						x1={23.982}
						x2={151.982}
						y1={56}
						y2={56}
						fill="none"
						stroke="#ec40c7"
						strokeLinecap="round"
						strokeLinejoin="round"
						strokeWidth={24}
					/>
					<Path
						fill="none"
						stroke="#ec40c7"
						strokeLinecap="round"
						strokeLinejoin="round"
						strokeWidth={24}
						d="M119.98242 56a96 96 0 0 1-96 96M64.69682 96.00062a96.01575 96.01575 0 0 0 87.27974 55.96606"
					/>
				</Svg>
			</Pressable>

			{openModal && (
				<View className=" absolute -left-6 top-0 flex items-center justify-center h-fit z-50">
					<View className="w-screen h-screen bg-black/70 fixed top-0 left-0 bottom-0 right-0" />
					<CustomModal openModal={openModal} setOpenModal={setOpenModal} />
				</View>
			)}
		</>
	)
}

export default TranslateBtn
