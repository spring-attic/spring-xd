---
layout: documentation_page
---
# Spring XD Architectures and Use Cases

* [Telco Services](#toc_1)
* [Health Care Services](#toc_7)
* [Energy Services](#toc_12)
* [Music Services](#toc_16)
* [Agriculture Services](#toc_20)
* [Airline Services](#toc_24)

## Telco Services

### Purpose
Contextually clever services that adapt to customer preferences along with geo-location into the mix improves operational efficiency. 

### Use Case
Telco service providers would like to be equipped with rich platform to reduce churn, launch new services on-demand, or create new revenue streams all in near real-time.

<img src="img/architectures-telco.png"/>

### Problem
Growing addition to network protocols, data formats, and the need to generate context specific customer outcomes in real-time is very challenging.

### Solution
The data-integration adapters in Spring XD allows subscription of various data sources. Equally seamless is to transform the individual payload into a desired data format. As the data is in transit, context specific data fragments can be computed through rich machine learning algorithms either through PMML models or via data processing engines such as Spark's MLLIb. Delegating the computation to Python is another alternative. 

### Outcome
Spring XD equips data scientists to collaborate seamlessly with Big Data Engineers and Application Developers.

## Health Care Services

### Purpose
Data from Historical health records, lifestyle changes and genetics – all relevant to proactive outbreak study and to use as patterns for symptom diagnosis and medical treatments.

### Use case
Medical professionals would like to get access to historical data to combine with current patient vitals in order to diagnose and prescribe correct treatment procedures. Given that information is readily available, medical professionals would like to use the "actual" data points so that they can quantify and qualify.

### Problem
Digital collection, storing and analysis of health records is cumbersome and equally challenging is protecting patient health records.

### Solution
Data pipelines in Spring XD are designed to isolate data movement and at the same time protect the information through enterprise-grade security features such as SSL handshakes, LDAP and Kerberos. Spring XD can be used to collate data from various client endpoints (ex: medical implants, hospital devices, HL7 standards, clinical equipment), store them in easily accessible data-marts, and run analytics workflows to compute real-time predictions.

### Outcome
Spring XD simplifies data collection and at the same provides fixtures to protect sensitive patient health records.

## Energy Services

### Purpose
Automation is critical for saving energy. Computerized electric utility grids yield immediate benefits through automated monitoring and management of connected-devices from a central location.

### Use case
Energy service provider would like to proactively monitor and react to failure conditions as quickly as possible. They would like to forecast utilization to enable customers consume energy efficiently. Under failure scenarios, the service provider would like to dispatch maintenance units and orchestrate the resolution lifecycle rapidly.

### Problem
Consuming data from various data devices is challenging and equally cumbersome is to make sense out of the data.

### Solution
Spring XD provides out of the box data-integration adapters that connect with data producing devices. As the data is in the pipeline, Spring XD also provides fixtures for data wrangling so that it is cleansed, transformed and analyzed as appropriate. Energy services providers are now equipped with rich platform to handle both online and offline data that can be used for data mining to produce predictive analytics models. The models can be introduced back in the data pipeline using Batch workflows to perform real-time predictions.

### Outcome
Spring XD provides fixtures for real-time predictions based on usage patterns and historical trends. Given the unified approach towards handling and analyzing data, Spring XD automates workflows to reduce manual intervention.

## Music Services

### Purpose
Growing music delivery platforms and trendy wearables are disrupting the way we consume music. At the same time, it creates room for more innovation.

### Use case
Music streaming providers would like to create a data-centric platform to engage consumers through persona and context specifics.  

### Problem
Attracting customers to a music platform by itself is one big challenge; retaining and engaging them with personalized services can get even more challenging.

### Solution
Individual persona characteristics can be derived from preferences, interests and recommendations. Contexts can be past listening trends, favorite music artist, rating on a album/song. It can also be a counter on number of times a song was played - it’s another data point. Using Spring XD, real-time pipelines can be created to compute taste and preferences to deliver real-time recommendations. Spring XD’s REST APIs can be leveraged to seamlessly interface with wearables to publish and listen to data events.

### Outcome
The out of the box adapters in Spring XD allows seamless interaction with ‘connected devices’.

## Agriculture Services

### Purpose
Better crop quality and optimized yields lead to sustainable energy and stronger economy.

### Use Case
Agribusiness providers would like to have a common data-exchange platform so that they can be connected with various service providers (ex: weather station, seed and fertilizer suppliers, machinery providers, and laboratories) to efficiently manage supply and demand.

### Problem
Agribusiness providers run their own supply and demand workflows. It’s often handled manually and it’s error prone. Interconnection between providers require everyone connected to a common platform in order to automate workflows. However, there exist numerous challenges to build one-stop-shop for all the data that can be accessible at anytime from anywhere.

### Solution
A data-pipeline for each interconnected data producing agents (ex: weather units, laboratories), enables easy collection of varied data sources. Spring XD covers both structured and unstructured data fragments. Once the data is in the pipeline, the out of the box analytics methods can be applied to compute predictions. The predictive models can be used for forecasting to enhance the overall agribusiness value chain.

### Outcome
Creating a data-exchange platform in Spring XD is straightforward. The ability to rapidly operationalize reliable data pipelines contributes to efficient data analysis and in turn better supply and demand management.

## Airline Services

### Use Case
Data influenced business decisions allow airline service providers benefit from increased customer loyalty, revenue margins, efficient fares, ticketing and inventory management.

### Problem
Silos of data fragments that are tightly coupled in countless channels of data-enabling applications are hard to consume is one thing; using it is another.

### Solution
Spring XD makes it easy to consume structured and unstructured data. Robust data transformation add-ons assist with data cleansing and formatting for eventual consistency. Such meaningful array of data fragments is now available for various automation and analysis ranging from baggage carousel optimization to adjusting itinerary based on weather patterns or seasonal rush.

