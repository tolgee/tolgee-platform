import { View } from "react-native"
import Svg, { Defs, RadialGradient, Rect, Stop } from "react-native-svg"

export default function RadialGradientComp({ classes, width, height }) {
	return (
		<View className={`absolute top-20 ${classes} shadow-xl`}>
			<Svg className={`${width ? width : "w-56"} ${height ? height : "h-56"}`}>
				<Defs>
					<RadialGradient
						id="grad"
						cx="50%"
						cy="50%"
						rx="50%"
						ry="50%"
						fx="50%"
						fy="50%"
						gradientUnits="objectBoundingBox"
					>
						<Stop offset="0%" stopColor="#ec40c7" stopOpacity=".9" />
						<Stop offset="100%" stopColor="transparent" stopOpacity="0" />
					</RadialGradient>
				</Defs>
				<Rect x="0" y="0" width="100%" height="100%" fill="url(#grad)" />
			</Svg>
		</View>
	)
}
