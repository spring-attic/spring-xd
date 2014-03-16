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

#load the iris data
data(iris)


#Split the iris dataset into test and train sets
splits <- splitDataFrame(iris, seed = 1337, n= round(0.95 * nrow(iris)))

#trainset
#testset


model <- lm(Petal.Width ~ Petal.Length, data=splits$trainset)

newdata <- data.frame(Petal.Length=4.5)

predicted <- predict(model, newdata)

# Export regression model as pmml
library(pmml)
pmml.lm(model)