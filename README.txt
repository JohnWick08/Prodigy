Context:
-   Our team has designed a table for our client to display all the products they registered on Amazon,
-   In this table, user can select a row (one product) to update its handling time / shipping template

Requirement:
-   User wants a button in the ui so that it is be able to all the products that has been selected at once
-   Amazon don't provide an api to update multiple product at once, it only supports update one at a time with a burst of 10 
    and rate of 5 per second. Need to prevent throttling and requests'completon needs to be logged 

Challenge:
-   Server down: If we are going to design a queue structure, the pending requests in this queue shouldn't disappear if our server 
    restarts, and this queue should be generic enough to handle requests for setting handling time and shipping template and
    future requests.
-   Ensure the queue is only being cleared by one process, and make sure requests that are added after the queue is being triggered 
    to clear will be cleared.

Design:
-   Create a new domain representing requests
-   To prevent throttling, make sure only one requests is processed once at a time, and the our rate should be less or equal to 5 per second.
-   To tackle the server-down challenge, set up a job that clears the pending requests queue once our server lauches
-   To tackle the queue clearing challenge, use an ReentrantLock to make sure the queue is only being cleared by one process, and use findBy() instead of findAllBy()
  
