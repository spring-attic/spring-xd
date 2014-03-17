library(e1071)
library(pmml)

data(iris)

#Splitts the given dataset in a training dataset(trainset) and test dataset(testset)
splitDataFrame <- function(dataframe, seed = NULL, n = trainSize) {

    if (!is.null(seed)){
       set.seed(seed)
    }

    index <- 1:nrow(dataframe)
    trainindex <- sample(index, n)
    trainset <- dataframe[trainindex, ]
    testset <- dataframe[-trainindex, ]

    list(trainset = trainset, testset = testset)
}


datasets <- splitDataFrame(iris, seed = 1337, n= round(0.95 * nrow(iris)))

#Create a naive Bayes classifier to predict iris flower species (iris[,5]) from [,1:4] = Sepal.Length Sepal.Width Petal.Length Petal.Width
model <- naiveBayes(datasets$trainset[,1:4], datasets$trainset[,5])

pmml(model,dataset=iris,predictedField="Species")