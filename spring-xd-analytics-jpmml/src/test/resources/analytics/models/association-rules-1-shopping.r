#see naive-bayes-classification-1-iris.r for split the shopping dataset in test and training set

#http://prdeepakbabu.wordpress.com/2010/11/13/market-basket-analysisassociation-rule-mining-using-r-package-arules/


install.packages("http://cran.r-project.org/web/packages/arules")
library(arules)

txn = read.transactions(file=”Transactions_sample.csv”, rm.duplicates= FALSE, format=”single”,sep=”,”,cols =c(1,2));
head(txn)

#http://stackoverflow.com/questions/20305083/can-not-coerce-list-with-transactions-with-duplicated-items-win7-sp1-64-r

> txn = read.transactions(file="transactions.csv", rm.duplicates= FALSE, format="single",sep=",",cols =c(1,2));

> basket_rules <- apriori(txn,parameter = list(sup = 0.5, conf = 0.9,target="rules"));

parameter specification:
 confidence minval smax arem  aval originalSupport support minlen maxlen target   ext
        0.9    0.1    1 none FALSE            TRUE     0.5      1     10  rules FALSE

algorithmic control:
 filter tree heap memopt load sort verbose
    0.1 TRUE TRUE  FALSE TRUE    2    TRUE

apriori - find association rules with the apriori algorithm
version 4.21 (2004.05.09)        (c) 1996-2004   Christian Borgelt
set item appearances ...[0 item(s)] done [0.00s].
set transactions ...[6 item(s), 7 transaction(s)] done [0.00s].
sorting and recoding items ... [2 item(s)] done [0.00s].
creating transaction tree ... done [0.00s].
checking subsets of size 1 2 done [0.00s].
writing ... [1 rule(s)] done [0.00s].
creating S4 object  ... done [0.00s].

> inspect(basket_rules)
  lhs            rhs        support confidence     lift
1 {Choclates} => {Pencil} 0.5714286          1 1.166667

> inspect(basket_rules[1])
  lhs            rhs        support confidence     lift
1 {Choclates} => {Pencil} 0.5714286          1 1.166667

> txn
transactions in sparse format with
 7 transactions (rows) and
 6 items (columns)

> inspect(txn)
  items       transactionID
1 {Choclates,
   Marker,
   Pencil}             1001
2 {Choclates,
   Pencil}             1002
3 {Coke,
   Eraser,
   Pencil}             1003
4 {Choclates,
   Cookies,
   Pencil}             1004
5 {Marker}             1005
6 {Marker,
   Pencil}             1006
7 {Choclates,
   Pencil}             1007

library(pmml)
pmml(basket_rules)
