# fulfilment-lookup

There are fulfilment files generated daily. They contain the information about who should receive a paper for that day 
 and where the paper should be delivered (and what subscription name that delivery corresponds to). These files are 
 provided to our delivery partners to let them know who should receive a paper, when, and where it should go.  

When a customer does not receive their paper as expected, it is useful to know if we included them in this file or not. 
This lambda checks, given a subscription name and a date, if there is an entry in the fulfilment file for that date.
Then, it automatically creates a salesforce case to indicate a delivery problem. 

#### What calls this lambda?

Imagine you are a paper subscriber, and your paper fails to arrive. You can then logon to 
`https://subscribe.theguardian.com/manage`

scroll down the page and see this (highlighted below) 'report a delivery problem' section

![image](https://user-images.githubusercontent.com/3072877/50844648-1403e380-1363-11e9-8000-d649e7955f09.png)

Specifying the date calls the lambda with your subscription name and the date your paper didn't arrive. This 
automatically raises a salesforce case to report your delivery problem and includes information about whether 
there is an entry for your subscription for that date in the fulfilment file for that day. 