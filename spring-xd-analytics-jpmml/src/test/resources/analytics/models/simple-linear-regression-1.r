year <- c(2000 ,   2001  ,  2002  ,  2003 ,   2004)
rate <- c(9.34 ,   8.50  ,  7.62  ,  6.93  ,  6.60)

sample <- data.frame(year,rate)

model <- lm(rate ~ year, sample)
newdata <- data.frame(year=2015)

predicted <- predict(model, newdata)

# In 2015 we have negative interest rates ;-)
predicted[1] // [1] -1.367

# Export regression model as pmml
library(pmml)
pmml.lm(model)