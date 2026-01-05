import React, { useEffect } from 'react';
import { TouchableOpacity, Text, View } from 'react-native';
import SurveySDK from './SurveySDK';

const SurveySDKButton = ({ 
  surveyId, 
  buttonId, 
  style, 
  textStyle, 
  children,
  onPress,
  onSurveyShown,
  onSurveyError 
}) => {
  
  const handlePress = async () => {
    try {
      if (onPress) {
        onPress();
      }
      
      if (surveyId) {
        const result = await SurveySDK.showSurveyById(surveyId);
        if (onSurveyShown) {
          onSurveyShown(result);
        }
      } else {
        const result = await SurveySDK.showSurvey();
        if (onSurveyShown) {
          onSurveyShown(result);
        }
      }
    } catch (error) {
      if (onSurveyError) {
        onSurveyError(error);
      }
      console.error('SurveySDK Button Error:', error);
    }
  };
  
  // Auto-register button if buttonId is provided
  useEffect(() => {
    if (buttonId) {
      // This would require additional native code to register custom buttons
      // For now, we'll just log it
      console.log(`SurveySDK Button registered with ID: ${buttonId}`);
    }
  }, [buttonId]);
  
  return (
    <TouchableOpacity 
      style={style}
      onPress={handlePress}
    >
      {children || <Text style={textStyle}>Take Survey</Text>}
    </TouchableOpacity>
  );
};

export default SurveySDKButton;