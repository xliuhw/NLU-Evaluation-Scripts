# README -- Scripts for evaluating NLU Services/Platforms
This project contains the Java codes for preparing NLU Services/Platforms data and evaluating the performances,
the Python scripts for qeurying NLU Services/Platforms. The NLU data are provided in [Here](https://github.com/xliuhw/NLU-Evaluation-Data)


## Content

### Java codes

  allGeneFileNames/ : contains the buffer file allGeneFileNames.txt shared between components.

  conf4RunRasaServer/ : the config file to run Rasa server for querying Rasa using the testset.

  Data_Info_BackedUp/testResults_Samples/ : 
        Sample prediction query output from each NLU services which are kept here
        for reference in case the services change their output formats and then
        the Evaluation.java should have corresponding changes.

  evaluateResults/ : (Empty now) Evaluation.java will write the results to here

  preprocessResults/ : (Empty now)  The PreprocessRealData.java and the processes for each services will save info here.
  
  testResults : (Empty now) the service query results will be saved here.

  resources/ : the java resources which also contains the released, annotated CSV data file.
  
  src/ : Java src
  
  target/ : Compiled Java target Jars. It will be overwritten each time when re-building the java package.
  
  targetWithDep: copied from target/ for running Jars from a terminal.
  
  pom.xml : The java Maven file.

### Python scripts:

  pythonQueryServices/ : the python scripts to query each service.

### Requirements

   JDK 1.8

   Netbeans 8.1

   Python 2.7

### Scripts usage examples
We used the mixed Java and Python scripts due to historical reasons. They should have been done in one language! Anyway, here is the example procedures to use the scripts for evaluating Dialogflow (formerly Apiai). The procedures for other Services/Platforms are similar, please refer to their specific requirements.

  1. Generate the trainsets and testsets.
  Run the file PreprocessRealData.java (In Netbean 8.1, right-click the file, select Run File)
  It will load the config file resources/config/Config_DataPreprocess.txt where the annontated data file is specified and generate the datasets in autoGeneFromRealAnno/preprocessResults/
  The config file also specifies how many utterances to use from the whole annontated csv file as the train set. It is maxNumUtt (for each intent).

  2. Convert the datasets to the right format of the Service.
  Run PrepareApiaiCorpora.java, it will load the generated datasets in Step1 above, convert them to Dialogflow format. The results will be saved in preprocessResults/out4ApiaiReal/

  3. Import the trainset to the Service.
   First Zip the "merged" directory in preprocessResults/out4ApiaiReal/Apiai_trainset_TheNewlyGeneratedTimeStamp/
to e.g. merged.zip
   Log into your Dialogflow account, click "Create new agent", in your newly created agent page, click Export and Import/IMPORT FROM ZIP, select merged.zip created above.
   Waiting for Dialogflow to finish training your agent.

  4. Test your agent using the generated testset in Step1
  cd pythonQueryServices/pythonApiai
  python queryApiai.py --testset default/specified --token YourClientToken
  This will query your agent and get the predictions of the testsets, and save the results to
  testResults/APIAI/
  NB: This may not work any more as the Dialogflow Python API has been changed. New python API will be needed to query Dialogflow.

   5. Evaluate the performance:
   Manually modify the relevant parts in the method doEvaluation_Manually_Top() in Evaluation.java and
Run the file Evaluation.java. The results will be saved in evaluateResults/APIAI/

   NB the buffer file allGeneFileNames/allGeneFileNames.txt will be using for the shared information between different steps.


## Contact
Please contact x.liu@hw.ac.uk, if you have any questions


