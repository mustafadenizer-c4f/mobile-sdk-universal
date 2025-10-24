module.exports = {
  dependency: {
    platforms: {
      android: {
        packageImportPath: 'import com.example.surveysdk.reactnative.SurveySDKPackage;',
        packageInstance: 'new SurveySDKPackage()'
      },
      ios: null
    }
  }
};