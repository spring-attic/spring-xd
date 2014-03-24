Spring XD Analytics ML - PMML
=============================

# Stream definition
Create the stream definition.
Note that you need to copy the iris-flower-classification-naive-bayes-1.pmml.xml file form the
spring-xd-analytics-ml-pmml/src/test/resources/pmml folder to the folder {XD_HOME}/analytics.

```
stream create
--name iris-flow-classification
--definition "http | analytic-pmml --analyticName='iris-flower-classification-naive-bayes-1' --inputFields='sepalLength:Sepal.Length,sepalWidth:Sepal.Width,petalLength:Petal.Length,petalWidth:Petal.Width' --outputFields='Predicted_Species:predictedSpecies' | log"
```

# Input
Post some data to the stream:

```
http post --target http://localhost:9000 --contentType application/x-xd-tuple --data "{ \"sepalLength\": 6.4, \"sepalWidth\": 3.2, \"petalLength\":4.5, \"petalWidth\":1.5 }"
```

# Ouptut
See the output in the log, note the generated field: "predictedSpecies":

```json
Output:
4/03/22 17:46:20 WARN logger.iris-flow-classification:
{
	"id":"7eeee430-b1e1-11e3-b13d-28cfe918b323"
	,"timestamp":1395506780659
	,"sepalLength":"6.4"
	,"sepalWidth":"3.2"
	,"petalLength":"4.5"
	,"petalWidth":"1.5"
	,"predictedSpecies":"setosa"
}
```