import { NextResponse } from "next/server";
import { GoogleGenerativeAI } from "@google/generative-ai";

export async function POST(req, res) {
  try {
    // Access your API key by creating an instance of GoogleGenerativeAI
    const genAI = new GoogleGenerativeAI(process.env.GOOGLE_API_KEY);

    // Initialize a generative model
    const model = genAI.getGenerativeModel({ model: "gemini-1.5-flash" });

    // Retrieve the data we receive as part of the request body
    const data = await req.json();
    const recipeName = data.recipeName;
    const language = data.language;
    console.log(recipeName, language);
    let prompt = "";
    const prompt1 = `Generate a Short recipe in ${language} language for the dish ${recipeName} in JSON format with these fields: 
          { 
            "recipename": "Recipe Name",
            "ingredients": ["ingredient1", "ingredient2", ...],
            "instructions": ["Step 1",\n "Step 2",\n ...],
            "tips": ["Tip 1",\n "Tip 2",\n ...]
          }`;
    const prompt2 = `Generate a Random Short recipe in ${language} language for a well-known dish ensure it is not repeated.in JSON format with these fields: 
          { 
            "recipename": "Recipe Name",
            "ingredients": ["ingredient1", "ingredient2", ...],
            "instructions": ["Step 1",\n "Step 2",\n ...],
            "tips": ["Tip 1",\n "Tip 2",\n ...]
          }`;
    if (recipeName == "Random") {
      prompt = prompt2;
    } else {
      prompt = prompt1;
    }
    // Pass the prompt to the model and retrieve the output in JSON format
    const result = await model.generateContent(prompt);

    const response = result.response;
    let output = response.text();
    console.log(output);

    let generatedData;
    try {
      // Removing extra code block markers if they exist (e.g., ```json)
      output = output.replace(/```json/g, "").replace(/```/g, "");

      // Attempt to parse the cleaned string into JSON
      generatedData = JSON.parse(output);
    } catch (jsonError) {
      console.error("Failed to parse JSON from AI response:", jsonError);
      return NextResponse.json({
        error: "Failed to generate recipe in proper JSON format.",
      });
    }
    console.log(generatedData);
    if (
      typeof generatedData.recipename !== "string" ||
      !Array.isArray(generatedData.ingredients) ||
      !Array.isArray(generatedData.instructions) ||
      !Array.isArray(generatedData.tips)
    ) {
      return NextResponse.json({
        error:
          "Generated recipe did not have the required fields or structure.",
      });
    }

    // Send the structured JSON response to the frontend
    return NextResponse.json({
      recipename: generatedData.recipename,
      ingredients: generatedData.ingredients,
      instructions: generatedData.instructions,
      tips: generatedData.tips,
    });
  } catch (error) {
    console.error("Error generating recipe:", error);
    return NextResponse.json({
      error: "An error occurred while generating the recipe.",
    });
  }
}
