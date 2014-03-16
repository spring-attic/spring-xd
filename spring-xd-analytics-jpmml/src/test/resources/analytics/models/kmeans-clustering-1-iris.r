#see naive-bayes-classification-1-iris.r for split the iris dataset in test and training set

# http://horicky.blogspot.de/2012/04/machine-learning-in-r-clustering.html

#Perform a k-means clustering on the training set with 3 clusters
model <- kmeans(datasets$trainset[,1:4], 3)

table(model$cluster, datasets$trainset$Species)

pmml(model)


> table(model$cluster, datasets$trainset$Species)

    setosa versicolor virginica
  1      0         40        14
  2     50          0         0
  3      0          3        35