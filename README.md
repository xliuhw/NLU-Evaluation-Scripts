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


## Contact
Please contact x.liu@hw.ac.uk, if you have any questions


