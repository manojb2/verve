Overview of Implementation Approach
In this document, we outline the implementation approach for the Java REST service designed to process at least 10,000 
requests per second while effectively tracking and sending unique request counts to a log file

Key Objectives
REST Service Functionality:

Create a REST service with a single GET endpoint (/api/verve/accept) that accepts an integer ID as a mandatory query parameter.
Ensure the service can handle high throughput while logging unique request counts.


Unique Count Management:
Use concurrent HashMap to track unique request IDs, ensuring deduplication in multithreaded environment

When running on Multiple server, I have implemented a different class with redis for caching unique requests count, as
when not using multiple servers redis will increase latency and is not required

The cache(concurrent hashMap/Redis) is created with string (time stamp ) as key, for every minute and set<Integers> as value 
to store unique values in that time interval. Later we can modify this by increasing time interval also. cache is storing for 10 mins
(can be changed later) and then deleted. number of unique requests are saved in logs every minute, later it can be changed and the cache 
stored for 10 mins can be pushed to kafka and different services can do the processing as needed

logs are stored in directory logs/ info and error logs are stored in different files, can be changed to add more logs

multithreading is implemented at http request when endpoint is provided as all other operations are not heavy

Testing:
tested with docker image when using concurrent hashMap,
tested locally when using redis
kafka can be implemented if required(not tested)

command to run 

docker compose up --build 

