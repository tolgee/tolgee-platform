'use client'

import React, { useState, useEffect, useRef } from 'react';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Slider } from '@/components/ui/slider';
import { Calendar, Paintbrush, RefreshCw, Download, Eraser, Undo } from 'lucide-react';
import { useTranslate } from '@tolgee/react';



const CANVAS_WIDTH = 800;
const CANVAS_HEIGHT = 600;

const DailyDrawingChallenge = () => {
  const { t } = useTranslate();
  
  const [drawingPrompts, setDrawingPrompts] = useState<string[]>([]);

  
  const [currentPrompt, setCurrentPrompt] = useState('');
  const [lastUpdated, setLastUpdated] = useState<Date>();
  const [isLoading, setIsLoading] = useState(false);
  const [isDrawing, setIsDrawing] = useState(false);
  const [brushColor, setBrushColor] = useState('#000000');
  const [brushSize, setBrushSize] = useState(5);
  const [isEraser, setIsEraser] = useState(false);
  
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const contextRef = useRef<CanvasRenderingContext2D | null>(null);
  const drawHistoryRef = useRef<ImageData[]>([]);
  const historyIndexRef = useRef(-1);

  useEffect(() => {
    // Set translated prompts after component mounts
    setDrawingPrompts([
      t('prompt1'),
      t('prompt2'),
      t('prompt3'),
      t('prompt4'),
      t('prompt5')
    ]);
    setCurrentPrompt(getRandomPrompt());
    setLastUpdated(new Date());
  }, [t]);

  // Initialize canvas
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    canvas.width = CANVAS_WIDTH;
    canvas.height = CANVAS_HEIGHT;
    
    const context = canvas.getContext('2d');
    if (!context) return;
    
    context.lineCap = 'round';
    context.lineJoin = 'round'; // Add this to smooth out line connections
    context.strokeStyle = brushColor;
    context.lineWidth = brushSize;
    context.fillStyle = 'white';
    context.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
    
    contextRef.current = context;
    saveToHistory();
  }, []);

  // Update context when brush properties change
  useEffect(() => {
    if (!contextRef.current) return;
    contextRef.current.strokeStyle = isEraser ? '#FFFFFF' : brushColor;
    contextRef.current.lineWidth = brushSize;
  }, [brushColor, brushSize, isEraser]);

  const getRandomPrompt = () => {
    const randomIndex = Math.floor(Math.random() * drawingPrompts.length);
    return drawingPrompts[randomIndex];
  };

  const refreshPrompt = () => {
    setIsLoading(true);
    setTimeout(() => {
      const randomIndex = Math.floor(Math.random() * drawingPrompts.length);
      setCurrentPrompt(drawingPrompts[randomIndex]);
      setLastUpdated(new Date());
      setIsLoading(false);
    }, 500);
  };



  // Enhanced drawing functions to improve precision
  const getCanvasCoordinates = (event: React.MouseEvent<HTMLCanvasElement>) => {
    const canvas = canvasRef.current;
    if (!canvas) return null;

    const rect = canvas.getBoundingClientRect();
    const scaleX = canvas.width / rect.width;
    const scaleY = canvas.height / rect.height;

    return {
      x: (event.clientX - rect.left) * scaleX,
      y: (event.clientY - rect.top) * scaleY
    };
  };

  const startDrawing = (event: React.MouseEvent<HTMLCanvasElement>) => {
    const coords = getCanvasCoordinates(event);
    if (!coords || !contextRef.current) return;

    contextRef.current.beginPath();
    contextRef.current.moveTo(coords.x, coords.y);
    setIsDrawing(true);
  };

  const draw = (event: React.MouseEvent<HTMLCanvasElement>) => {
    if (!isDrawing || !contextRef.current) return;

    const coords = getCanvasCoordinates(event);
    if (!coords) return;

    contextRef.current.lineTo(coords.x, coords.y);
    contextRef.current.stroke();
  };

  const stopDrawing = () => {
    if (!contextRef.current) return;
    contextRef.current.closePath();
    setIsDrawing(false);
    saveToHistory();
  };

  const saveToHistory = () => {
    if (!canvasRef.current || !contextRef.current) return;
    
    const imageData = contextRef.current.getImageData(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
    historyIndexRef.current++;
    drawHistoryRef.current = drawHistoryRef.current.slice(0, historyIndexRef.current);
    drawHistoryRef.current.push(imageData);
  };

  const undo = () => {
    if (!contextRef.current || historyIndexRef.current <= 0) return;
    
    historyIndexRef.current--;
    const imageData = drawHistoryRef.current[historyIndexRef.current];
    if (imageData) {
      contextRef.current.putImageData(imageData, 0, 0);
    }
  };

  const downloadDrawing = () => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const dataUrl = canvas.toDataURL('image/png');
    const link = document.createElement('a');
    link.href = dataUrl;
    link.download = `drawing-${new Date().toISOString()}.png`;
    link.click();
  };

  const clearCanvas = () => {
    if (!contextRef.current || !canvasRef.current) return;
    contextRef.current.fillStyle = 'white';
    contextRef.current.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
    saveToHistory();
  };

  return (
    <div className="w-full max-w-4xl mx-auto space-y-4 p-4">
      <Card className="shadow-lg hover:shadow-xl transition-shadow duration-300">
        <CardHeader className="bg-purple-50/50 border-b border-purple-100">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <Paintbrush className="h-7 w-7 text-purple-600" />
              <CardTitle className="text-2xl font-bold text-purple-900">{t('app-name')}</CardTitle>
            </div>
            <Button 
              variant="outline" 
              size="icon"
              onClick={refreshPrompt}
              disabled={isLoading}
              className="hover:bg-purple-100 transition-colors"
            >
              <RefreshCw className={`h-5 w-5 ${isLoading ? 'animate-spin' : 'text-purple-600'}`} />
            </Button>
          </div>
          <CardDescription className="text-purple-700/80">{t('taglline')}</CardDescription>
        </CardHeader>
        
        <CardContent className="p-6">
          <div className="bg-purple-50 p-6 rounded-lg text-center mb-6 shadow-sm">
            <p className="text-2xl font-semibold text-purple-900 italic">{currentPrompt}</p>
          </div>

          <div className="space-y-6">
            <div className="flex items-center space-x-4">
              <div className="border-2 rounded-lg p-1 shadow-sm">
                <input
                  type="color"
                  value={brushColor}
                  onChange={(e) => setBrushColor(e.target.value)}
                  className="w-12 h-12 rounded cursor-pointer"
                />
              </div>
              <div className="flex-1">
                <p className="text-sm text-gray-600 mb-2">{t('brush-size')}: {brushSize}px</p>
                <Slider
                  value={[brushSize]}
                  onValueChange={(value) => setBrushSize(value[0])}
                  min={1}
                  max={50}
                  step={1}
                  className="w-full"
                />
              </div>
            </div>

            <div className="flex space-x-2">
              <Button
                variant={isEraser ? "default" : "outline"}
                onClick={() => setIsEraser(!isEraser)}
                className="hover:bg-purple-500 transition-colors"
              >
                <Eraser className="h-4 w-4 mr-2" />
                {isEraser ? t('drawingMode') : t('eraser')}
              </Button>
              <Button 
                variant="outline" 
                onClick={undo}
                className="hover:bg-gray-100 transition-colors"
              >
                <Undo className="h-4 w-4 mr-2" />
                {t('undo')}
              </Button>
              <Button 
                variant="outline" 
                onClick={clearCanvas}
                className="hover:bg-red-100 transition-colors"
              >
                {t('clear')}
              </Button>
              <Button 
                variant="default" 
                onClick={downloadDrawing}
                className="bg-purple-600 hover:bg-purple-700 transition-colors"
              >
                <Download className="h-4 w-4 mr-2" />
                {t('download')}
              </Button>
            </div>

            <div className="border-2 border-purple-100 rounded-lg p-2 bg-white shadow-sm">
              <canvas
                ref={canvasRef}
                onMouseDown={startDrawing}
                onMouseMove={draw}
                onMouseUp={stopDrawing}
                onMouseLeave={stopDrawing}
                className="w-full border-2 border-purple-50 bg-white cursor-crosshair"
                style={{ aspectRatio: `${CANVAS_WIDTH}/${CANVAS_HEIGHT}` }}
              />
            </div>
          </div>
        </CardContent>
        
        <CardFooter className="bg-purple-50/50 border-t border-purple-100 p-4">
          <div className="flex items-center space-x-2 text-gray-600">
            <Calendar className="h-5 w-5 text-purple-600" />
            <span className="text-sm">
            {t('date')}: {lastUpdated?.toLocaleDateString()} {lastUpdated?.toLocaleTimeString()}
            </span>
          </div>
        </CardFooter>
      </Card>
    </div>
  );
};

export default DailyDrawingChallenge;