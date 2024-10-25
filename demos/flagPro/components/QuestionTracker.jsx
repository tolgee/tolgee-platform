import { View } from "react-native"
import React from "react"

const QuestionTracker = ({ quesId }) => {
	const width = (100 / 6) * parseInt(quesId) + "%"
	return (
		<View className="w-full h-2 z-40 bg-white overflow-hidden rounded-md  ">
			<View
				className={`bg-grad h-full z-10 left-0 absolute top-0`}
				style={{ width: width }}
			/>
		</View>
	)
}

export default QuestionTracker
