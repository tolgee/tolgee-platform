import LottieView from "lottie-react-native"
import { StyleSheet } from "react-native"
const Trophy = () => {
	return (
		<LottieView
			source={require("../assets/trophy.json")} // Path to your animation file
			autoPlay
			loop
			style={styles.animation}
		/>
	)
}

const styles = StyleSheet.create({
	container: {
		flex: 1,
		justifyContent: "center",
		alignItems: "center",
		backgroundColor: "#fff",
	},
	animation: {
		width: 300, // Set width and height as needed
		height: 300,
	},
})

export default Trophy
