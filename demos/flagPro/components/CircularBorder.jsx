import React from "react"
import { View } from "react-native"
import Svg, { Circle } from "react-native-svg"

const CircularBorder = () => {
	return (
		<View className="absolute top-1/4 left-1/4 w-full h-full -translate-x-1/4 -translate-y-1/4">
			<Svg>
				<Circle
					cx="27%" // x-coordinate of the circle's center
					cy="23%" // y-coordinate of the circle's center
					r="100" // radius of the circle
					stroke="#ec407a" // border color
					strokeWidth="10" // width of the border
					fill="transparent" // fill color inside the circle
				/>
			</Svg>
		</View>
	)
}

export default CircularBorder
